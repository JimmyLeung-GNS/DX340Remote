@file:Suppress("DEPRECATION")
package org.dx340.remote.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import org.dx340.remote.protocol.Reassembler
import org.dx340.remote.protocol.Wire
import org.dx340.remote.protocol.IndicationQueue
import java.util.UUID

@SuppressLint("MissingPermission")
class BlePeripheral(
    private val context: Context,
    private val status: (String) -> Unit,
    private val previouslyApproved: (BluetoothDevice) -> Boolean,
    private val disconnected: (BluetoothDevice) -> Unit,
    private val message: (BluetoothDevice, ByteArray) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private var server: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var peer: BluetoothDevice? = null
    private var subscribed = false
    private var capacity = 20
    private var serial = 0
    private val outgoing = IndicationQueue(
        schedule = { delay, action ->
            val task = Runnable { action() }
            main.postDelayed(task, delay)
            val cancel: () -> Unit = { main.removeCallbacks(task) }; cancel
        },
        submit = ::sendFrame,
        failed = { reason -> peer?.let { closePeer(it, reason) } }
    )
    private val assembler = Reassembler(android.os.SystemClock::elapsedRealtime)
    var pairingUntil = 0L
    private val tx = BluetoothGattCharacteristic(UUID.fromString(Wire.TX), BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM).apply {
        addDescriptor(BluetoothGattDescriptor(UUID.fromString(Wire.CCC), BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM))
    }
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) { main.post { if (server != null) status("ADVERTISING: GATT service active") } }
        override fun onStartFailure(errorCode: Int) { main.post { if (server != null) status("ADVERTISING_FAILED:$errorCode") } }
    }
    private val callback = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(code: Int, service: BluetoothGattService) { main.post {
            if (server == null) return@post
            if (code != BluetoothGatt.GATT_SUCCESS) { status("GATT_SERVICE_FAILED:$code"); return@post }
            advertiser?.startAdvertising(AdvertiseSettings.Builder().setConnectable(true).setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY).build(),
                AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.fromString(Wire.SERVICE))).build(), advertiseCallback)
        } }
        override fun onConnectionStateChange(device: BluetoothDevice, code: Int, newState: Int) { main.post {
            if (server == null) return@post
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (code != BluetoothGatt.GATT_SUCCESS) {
                    disconnect(device); status("CONNECTION_FAILED:$code"); return@post
                }
                if ((peer != null && peer != device) || ((!previouslyApproved(device) || device.bondState != BluetoothDevice.BOND_BONDED) && android.os.SystemClock.elapsedRealtime() > pairingUntil)) {
                    server?.cancelConnection(device); return@post
                }
                forgetPeer()
                peer = device
                status("CONNECTED: awaiting encrypted subscription and approval")
            } else if (peer == device) {
                forgetPeer(); status("ADVERTISING: controller disconnected, playback unchanged")
            }
        } }
        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) { main.post { if (device == peer) capacity = (mtu - 3).coerceIn(20, 512) } }
        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) { main.post {
            val bytes = "{\"v\":1,\"security\":\"bond-mitm-and-local-approval\",\"maxMessage\":65536}".toByteArray()
            if (characteristic.uuid.toString() != Wire.INFO || offset !in 0..bytes.size) server?.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
            else server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, bytes.copyOfRange(offset, bytes.size))
        } }
        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor) { main.post {
            server?.sendResponse(device, requestId, if (offset == 0) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_INVALID_OFFSET, offset,
                if (subscribed && device == peer) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } }
        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) { main.post {
            val ok = device == peer && device.bondState == BluetoothDevice.BOND_BONDED && descriptor.uuid.toString() == Wire.CCC && !preparedWrite && offset == 0 &&
                (value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) || value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE))
            if (ok) {
                subscribed = value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                if (subscribed) outgoing.open()
            }
            if (responseNeeded) server?.sendResponse(device, requestId, if (ok) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE, 0, null)
            if (ok) {
                if (subscribed) status("ENCRYPTED_SUBSCRIPTION: local approval required for new controller")
                else closePeer(device, "SUBSCRIPTION_DISABLED: authorization and queued replies cleared")
            }
        } }
        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) { main.post {
            var code = BluetoothGatt.GATT_SUCCESS
            var complete: ByteArray? = null
            if (device != peer || !subscribed || device.bondState != BluetoothDevice.BOND_BONDED || characteristic.uuid.toString() != Wire.RX || preparedWrite || offset != 0 || !responseNeeded) code = BluetoothGatt.GATT_FAILURE
            else try { complete = assembler.accept(value) } catch (_: IllegalArgumentException) { code = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH }
            if (responseNeeded) server?.sendResponse(device, requestId, code, offset, null)
            if (code == BluetoothGatt.GATT_SUCCESS) complete?.let { message(device, it) }
        } }
        override fun onNotificationSent(device: BluetoothDevice, code: Int) { main.post {
            if (device != peer) return@post
            outgoing.complete(if (code == BluetoothGatt.GATT_SUCCESS) null else "INDICATION_FAILED:$code")
        } }
    }
    fun start() {
        check(server == null) { "Already running" }
        val adapter = manager.adapter ?: error("BLE_UNAVAILABLE")
        check(adapter.isEnabled) { "BLUETOOTH_OFF" }
        advertiser = adapter.bluetoothLeAdvertiser ?: error("ADVERTISER_UNAVAILABLE")
        status("BLE supported; multipleAdvertisement=${adapter.isMultipleAdvertisementSupported}")
        server = manager.openGattServer(context, callback) ?: error("GATT_SERVER_UNAVAILABLE")
        val service = BluetoothGattService(UUID.fromString(Wire.SERVICE), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        service.addCharacteristic(BluetoothGattCharacteristic(UUID.fromString(Wire.INFO), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM))
        service.addCharacteristic(BluetoothGattCharacteristic(UUID.fromString(Wire.RX), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM))
        service.addCharacteristic(tx)
        check(server!!.addService(service)) { "GATT_ADD_FAILED" }
    }
    fun send(device: BluetoothDevice, bytes: ByteArray, lowPriority: Boolean = false) {
        if (device != peer || !subscribed) return
        val frames = Wire.frames(bytes, capacity, serial++ and 65535)
        outgoing.enqueue(frames, lowPriority)
    }
    private fun sendFrame(bytes: ByteArray): String? {
        val device = peer ?: return "NOT_CONNECTED"
        if (!subscribed) return "NOT_SUBSCRIBED"
        val code = if (android.os.Build.VERSION.SDK_INT >= 33) server?.notifyCharacteristicChanged(device, tx, true, bytes)
        else { tx.value = bytes; if (server?.notifyCharacteristicChanged(device, tx, true) == true) 0 else -1 }
        return if (code == BluetoothGatt.GATT_SUCCESS) null else "SEND_FAILED:$code"
    }
    private fun forgetPeer(): BluetoothDevice? {
        val previous = peer
        peer = null; subscribed = false; capacity = 20
        outgoing.close(); assembler.clear()
        previous?.let(disconnected)
        return previous
    }
    private fun closePeer(device: BluetoothDevice, reason: String) {
        if (device != peer) return
        // Invalidate app authorization now, even when Android's disconnect callback is late.
        forgetPeer()
        status(reason)
        runCatching { server?.cancelConnection(device) }
    }
    fun disconnect(device: BluetoothDevice) {
        if (device == peer) forgetPeer()
        runCatching { server?.cancelConnection(device) }
    }
    fun disconnectController(address: String) { peer?.takeIf { it.address == address }?.let(::disconnect) }
    fun disconnectCurrent() { peer?.let(::disconnect) }
    fun stop() {
        val previousServer = server
        server = null
        val device = forgetPeer()
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }; advertiser = null
        runCatching { device?.let { previousServer?.cancelConnection(it) } }
        runCatching { previousServer?.close() }
        main.removeCallbacksAndMessages(null)
    }
}

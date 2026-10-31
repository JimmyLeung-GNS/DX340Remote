@file:Suppress("DEPRECATION")
package org.dx340.remote.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import org.dx340.remote.protocol.Reassembler
import org.dx340.remote.protocol.Wire
import org.dx340.remote.protocol.PeerAdmission
import org.dx340.remote.protocol.PeerIndicationQueue
import org.dx340.remote.protocol.PeerWriteCapacity
import java.util.UUID

@SuppressLint("MissingPermission")
class BlePeripheral(
    private val context: Context,
    private val status: (String) -> Unit,
    private val previouslyApproved: (BluetoothDevice) -> Boolean,
    private val disconnected: (BluetoothDevice) -> Unit,
    private val message: (BluetoothDevice, ByteArray) -> Unit,
    // Production callers leave this false. The host debug variant can exercise
    // the real deadline after a deliberately ignored successful callback.
    private val ignoreCompletion: (Any, Boolean) -> Boolean = { _, _ -> false }
) {
    private class Link(val device: BluetoothDevice) {
        var subscribed = false
        var capacity = 20
        var serial = 0
        var controlling = false
        var expiry: Runnable? = null
        val assembler = Reassembler(SystemClock::elapsedRealtime)
    }
    private val main = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private var server: BluetoothGattServer? = null
    val isRunning get() = server != null
    private var advertiser: BluetoothLeAdvertiser? = null
    private val peers = linkedMapOf<BluetoothDevice, Link>()
    private val outgoing = PeerIndicationQueue<Link>(
        schedule = { delay, action ->
            val task = Runnable { action() }; main.postDelayed(task, delay)
            val cancel: () -> Unit = { main.removeCallbacks(task) }; cancel
        },
        submit = ::sendFrame,
        failed = { link, reason ->
            if (reason == "INDICATION_TIMEOUT") {
                stop(); status("BLE_REPLY_TIMEOUT: stop and start remote control on DX340; playback unchanged")
            } else closeLink(link, reason)
        }
    )
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
            val nameBytes = manager.adapter?.name?.toByteArray(Charsets.UTF_8)?.size ?: 0
            val scanResponse = if (nameBytes in 1..29) AdvertiseData.Builder().setIncludeDeviceName(true).build() else null
            advertiser?.startAdvertising(AdvertiseSettings.Builder().setConnectable(true).setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY).build(),
                AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.fromString(Wire.SERVICE))).build(), scanResponse, advertiseCallback)
        } }
        override fun onConnectionStateChange(device: BluetoothDevice, code: Int, newState: Int) { main.post {
            if (server == null) return@post
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (code != BluetoothGatt.GATT_SUCCESS) { disconnect(device); status("CONNECTION_FAILED:$code"); return@post }
                // Android exposes no link generation on this callback. Never
                // carry a controlling lease across an ambiguous repeated event.
                peers[device]?.let { closeLink(it, "DUPLICATE_CONNECTION_STATE"); return@post }
                val draining = outgoing.inFlightPeer?.device == device
                if (draining || !PeerAdmission.allowed(peers.size, previouslyApproved(device), device.bondState == BluetoothDevice.BOND_BONDED, SystemClock.elapsedRealtime() <= pairingUntil)) {
                    server?.cancelConnection(device); return@post
                }
                val link = Link(device)
                if (!outgoing.open(link)) { server?.cancelConnection(device); return@post }
                peers[device] = link; expireBootstrap(link, 60000)
                status("CONNECTED: awaiting encrypted subscription and approval; transports=${peers.size}")
            } else peers[device]?.let { forget(it); status("ADVERTISING: controller disconnected, playback unchanged") }
        } }
        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) { main.post { peers[device]?.capacity = (mtu - 3).coerceIn(20, 512) } }
        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) { main.post {
            val bytes = "{\"v\":1,\"security\":\"bond-mitm-and-local-approval\",\"maxMessage\":65536}".toByteArray()
            val code = when {
                device !in peers || device.bondState != BluetoothDevice.BOND_BONDED -> BluetoothGatt.GATT_FAILURE
                characteristic.uuid.toString() != Wire.INFO || offset !in 0..bytes.size -> BluetoothGatt.GATT_INVALID_OFFSET
                else -> BluetoothGatt.GATT_SUCCESS
            }
            server?.sendResponse(device, requestId, code, offset, if (code == BluetoothGatt.GATT_SUCCESS) bytes.copyOfRange(offset, bytes.size) else null)
        } }
        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor) { main.post {
            val link = peers[device]
            val ok = link != null && device.bondState == BluetoothDevice.BOND_BONDED && descriptor.uuid.toString() == Wire.CCC && offset == 0
            server?.sendResponse(device, requestId, if (ok) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE, offset,
                if (ok && link?.subscribed == true) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } }
        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) { main.post {
            val link = peers[device]
            val ok = link != null && device.bondState == BluetoothDevice.BOND_BONDED && descriptor.uuid.toString() == Wire.CCC && !preparedWrite && offset == 0 &&
                (value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) || value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE))
            if (ok) link!!.subscribed = value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            if (responseNeeded) server?.sendResponse(device, requestId, if (ok) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE, 0, null)
            if (ok) {
                if (link!!.subscribed) status("ENCRYPTED_SUBSCRIPTION: authorization required")
                else closeLink(link, "SUBSCRIPTION_DISABLED: authorization and queued replies cleared")
            }
        } }
        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) { main.post {
            val link = peers[device]
            var code = BluetoothGatt.GATT_SUCCESS
            var complete: ByteArray? = null
            if (link == null || !link.subscribed || device.bondState != BluetoothDevice.BOND_BONDED || characteristic.uuid.toString() != Wire.RX || preparedWrite || offset != 0 || !responseNeeded) code = BluetoothGatt.GATT_FAILURE
            else try {
                complete = link.assembler.accept(value)
                // A recreated server can attach to an already negotiated ACL
                // without receiving onMtuChanged. A received ordinary write is
                // a lower bound for the shared ATT payload, never an MTU guess.
                val observed = PeerWriteCapacity.observe(link.capacity, value.size)
                if (observed != link.capacity) {
                    link.capacity = observed
                    status("PAYLOAD_CAPACITY:$observed from received write")
                }
            } catch (_: IllegalArgumentException) { code = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH }
            if (responseNeeded) server?.sendResponse(device, requestId, code, offset, null)
            if (code == BluetoothGatt.GATT_SUCCESS) complete?.let { message(device, it) }
        } }
        override fun onNotificationSent(device: BluetoothDevice, code: Int) { main.post {
            val submitted = outgoing.inFlightPeer?.takeIf { it.device == device } ?: return@post
            if (ignoreCompletion(submitted, code == BluetoothGatt.GATT_SUCCESS)) {
                status("DIAGNOSTIC_CALLBACK_IGNORED: injected missed indication completion; awaiting existing deadline")
                return@post
            }
            outgoing.complete(submitted, if (code == BluetoothGatt.GATT_SUCCESS) null else "INDICATION_FAILED:$code")
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
    fun setControlling(device: BluetoothDevice, controlling: Boolean) {
        val link = peers[device] ?: return
        link.controlling = controlling
        link.expiry?.let(main::removeCallbacks); link.expiry = null
        if (!controlling) expireBootstrap(link, 10000)
    }
    private fun expireBootstrap(link: Link, delay: Long) {
        link.expiry?.let(main::removeCallbacks)
        link.expiry = Runnable { if (peers[link.device] === link && !link.controlling) closeLink(link, "BOOTSTRAP_TIMEOUT") }.also { main.postDelayed(it, delay) }
    }
    fun discardQueued(device: BluetoothDevice) { peers[device]?.let(outgoing::discardQueued) }
    /** Opaque link identity; never grants access or alters the indication queue. */
    fun idleControllerToken(device: BluetoothDevice): Any? = peers[device]?.takeIf {
        server != null && it.controlling && it.subscribed && outgoing.pendingCount == 0
    }
    fun send(device: BluetoothDevice, bytes: ByteArray, lowPriority: Boolean = false) {
        val link = peers[device]?.takeIf { it.subscribed } ?: return
        outgoing.enqueue(link, Wire.frames(bytes, link.capacity, link.serial++ and 65535), lowPriority)
    }
    private fun sendFrame(link: Link, bytes: ByteArray): String? {
        if (peers[link.device] !== link || !link.subscribed) return "NOT_SUBSCRIBED"
        val code = if (android.os.Build.VERSION.SDK_INT >= 33) server?.notifyCharacteristicChanged(link.device, tx, true, bytes)
        else { tx.value = bytes; if (server?.notifyCharacteristicChanged(link.device, tx, true) == true) 0 else -1 }
        return if (code == BluetoothGatt.GATT_SUCCESS) null else "SEND_FAILED:$code"
    }
    private fun forget(link: Link) {
        if (peers[link.device] !== link) return
        peers.remove(link.device); link.expiry?.let(main::removeCallbacks)
        outgoing.close(link); link.assembler.clear(); disconnected(link.device)
    }
    private fun closeLink(link: Link, reason: String) {
        if (peers[link.device] !== link) return
        forget(link); status(reason); runCatching { server?.cancelConnection(link.device) }
    }
    fun disconnect(device: BluetoothDevice) { peers[device]?.let(::forget); runCatching { server?.cancelConnection(device) } }
    fun disconnectController(address: String) { peers.keys.firstOrNull { it.address == address }?.let(::disconnect) }
    fun disconnectCurrent() { peers.keys.toList().forEach(::disconnect) }
    fun stop() {
        val previousServer = server; server = null
        val devices = peers.keys.toList(); peers.values.toList().forEach(::forget); outgoing.stop()
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }; advertiser = null
        devices.forEach { runCatching { previousServer?.cancelConnection(it) } }
        runCatching { previousServer?.close() }; main.removeCallbacksAndMessages(null)
    }
}

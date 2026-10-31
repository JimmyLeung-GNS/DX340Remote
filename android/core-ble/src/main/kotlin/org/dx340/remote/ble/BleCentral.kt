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
import org.dx340.remote.protocol.AcknowledgedWriteQueue
import org.dx340.remote.protocol.Wire
import java.util.UUID

enum class CentralPhase { SEARCHING, CONNECTING, CHECKING, RETRYING, FAILED, IDLE }

@SuppressLint("MissingPermission")
class BleCentral(private val context: Context, private val status: (String) -> Unit, private val discovered: (BluetoothDevice) -> Unit,
                 private val ready: () -> Unit, private val message: (ByteArray) -> Unit,
                 private val phase: (CentralPhase) -> Unit = {}, private val lost: () -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val adapter get() = context.getSystemService(BluetoothManager::class.java).adapter
    private var gatt: BluetoothGatt? = null
    private var rx: BluetoothGattCharacteristic? = null
    private var tx: BluetoothGattCharacteristic? = null
    private var capacity = 20
    private var serial = 0
    private val writes = AcknowledgedWriteQueue<() -> Unit>()
    private var established = false
    private var desired: BluetoothDevice? = null
    private var retries = 0
    private var reconnect: Runnable? = null
    private var writeGeneration = 0L
    private val assembler = Reassembler(android.os.SystemClock::elapsedRealtime)
    private var activeScan: ScanCallback? = null
    private var scanExpiry: Runnable? = null
    val connectedDevice get() = gatt?.device
    private val cb = object : BluetoothGattCallback() {
        override fun onServiceChanged(g: BluetoothGatt) { main.post {
            if (g != gatt) return@post
            // Cached handles are invalid. Reconnect and discover/authorize anew;
            // fail clears pending writes rather than replaying playback commands.
            fail("Player service changed. Reconnecting without replay.")
        } }
        override fun onConnectionStateChange(g: BluetoothGatt, code: Int, newState: Int) { main.post {
            if (g != gatt) { g.close(); return@post }
            if (code == 0 && newState == BluetoothProfile.STATE_CONNECTED) {
                phase(CentralPhase.CHECKING); status("Discovering service…"); if (!g.discoverServices()) fail("Service discovery could not start")
            } else { fail("Disconnected ($code). Commands cleared; reconnect to refresh.") }
        } }
        override fun onServicesDiscovered(g: BluetoothGatt, code: Int) { main.post {
            if (g != gatt) return@post
            val s = g.getService(UUID.fromString(Wire.SERVICE))
            rx = s?.getCharacteristic(UUID.fromString(Wire.RX)); tx = s?.getCharacteristic(UUID.fromString(Wire.TX))
            if (code != 0 || rx == null || tx == null) { fail("DX340 service unavailable"); return@post }
            // Serialize MTU negotiation before the CCC write; never assume 512 bytes.
            if (!g.requestMtu(247)) subscribe(g)
        } }
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, code: Int) { main.post {
            if (g != gatt) return@post
            capacity = if (code == 0) (mtu - 3).coerceIn(20, 512) else 20; subscribe(g)
        } }
        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, code: Int) { main.post {
            if (g != gatt) return@post
            if (code != 0) { desired = null; fail("Encrypted pairing failed ($code). Open pairing on DX340 and retry."); return@post }
            established = true; phase(CentralPhase.CHECKING); retries = 0; status("Connected; checking authorization…"); ready()
        } }
        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, code: Int) { main.post {
            if (g != gatt || c.uuid.toString() != Wire.RX || !writes.busy) return@post
            if (code != 0) fail("Bluetooth write failed ($code); outcome unknown") else {
                writes.acknowledge()?.invoke(); pump()
            }
        } }
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) { receive(g, value) }
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) { if (android.os.Build.VERSION.SDK_INT < 33) receive(g, c.value) }
    }
    private fun receive(g: BluetoothGatt, value: ByteArray) { main.post {
        if (g != gatt) return@post
        try { assembler.accept(value)?.let(message) } catch (_: IllegalArgumentException) { fail("Malformed Bluetooth reply; reconnect") }
    } }
    private fun subscribe(g: BluetoothGatt) {
        val characteristic = tx ?: return
        val d = characteristic.getDescriptor(UUID.fromString(Wire.CCC)) ?: return fail("Missing indications descriptor")
        if (!g.setCharacteristicNotification(characteristic, true)) return fail("Cannot subscribe")
        val result = if (android.os.Build.VERSION.SDK_INT >= 33) g.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
        else { d.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE; if (g.writeDescriptor(d)) 0 else -1 }
        if (result != 0) fail("Cannot start secure subscription ($result)")
    }
    fun scan(completed: ((Boolean) -> Unit)? = null) {
        stopScan(); check(adapter?.isEnabled == true) { "Enable Bluetooth first" }
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) { main.post { if (activeScan === this) discovered(result.device) } }
            override fun onScanFailed(errorCode: Int) { main.post {
                if (activeScan !== this) return@post
                stopScan(); phase(CentralPhase.FAILED); status("Search failed ($errorCode). Try connecting again."); completed?.invoke(false)
            } }
        }
        activeScan = callback
        try {
            adapter.bluetoothLeScanner.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(Wire.SERVICE))).build()), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        } catch (error: Exception) { stopScan(); throw error }
        phase(CentralPhase.SEARCHING); status("Searching for compatible players…")
        scanExpiry = Runnable {
            if (activeScan === callback) { stopScan(); completed?.invoke(true) }
        }.also { main.postDelayed(it, 10000) }
    }
    fun stopScan() {
        scanExpiry?.let(main::removeCallbacks); scanExpiry = null
        val callback = activeScan; activeScan = null
        if (callback != null) runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
    }
    fun connect(device: BluetoothDevice) {
        stopScan(); disconnect(); desired = device; retries = 0; connectAttempt(device)
    }
    private fun connectAttempt(device: BluetoothDevice) {
        phase(CentralPhase.CONNECTING); status("Connecting…")
        gatt = device.connectGatt(context, false, cb, BluetoothDevice.TRANSPORT_LE)
        val current = gatt
        main.postDelayed({ if (gatt == current && !established) fail("Connection timed out. Check phone control is enabled in Companion on your player, then tap Connect again.") }, 30000)
    }
    fun send(bytes: ByteArray, submitted: (() -> Unit)? = null, acknowledged: (() -> Unit)? = null) {
        check(established) { "Not connected" }
        val frames = Wire.frames(bytes, capacity, serial++ and 65535)
        writes.enqueue(frames, acknowledged, submitted); pump()
    }
    private fun pump() {
        val g = gatt ?: return; val c = rx ?: return
        val frame = writes.next() ?: return
        val generation = ++writeGeneration
        main.postDelayed({ if (writes.busy && g == gatt && generation == writeGeneration) fail("Bluetooth write timed out; outcome unknown. Reconnecting without replay.") }, 10000)
        val result = if (android.os.Build.VERSION.SDK_INT >= 33) g.writeCharacteristic(c, frame, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        else { c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT; c.value = frame; if (g.writeCharacteristic(c)) 0 else -1 }
        if (result != 0) fail("Could not queue command; reconnect") else writes.submitted()?.invoke()
    }
    private fun fail(reason: String) {
        clearLink(); status(reason)
        val device = desired
        if (device != null && retries < 5 && adapter?.isEnabled == true) {
            phase(CentralPhase.RETRYING)
            val delay = minOf(15000L, 1000L shl retries++)
            reconnect?.let { main.removeCallbacks(it) }
            reconnect = Runnable { if (desired == device && gatt == null) connectAttempt(device) }.also { main.postDelayed(it, delay) }
        } else phase(CentralPhase.FAILED)
    }
    fun pauseReconnection() { desired = null; retries = 0; reconnect?.let { main.removeCallbacks(it) }; reconnect = null }
    fun resumeReconnection() { if (established) { desired = gatt?.device; retries = 0 } }
    fun disconnect() {
        desired = null; retries = 0; reconnect?.let { main.removeCallbacks(it) }; reconnect = null; clearLink(); phase(CentralPhase.IDLE)
    }
    private fun clearLink() {
        val old = gatt; gatt = null; rx = null; tx = null; established = false; capacity = 20; writeGeneration++
        writes.clear(); assembler.clear(); runCatching { old?.disconnect() }; runCatching { old?.close() }; lost()
    }
}

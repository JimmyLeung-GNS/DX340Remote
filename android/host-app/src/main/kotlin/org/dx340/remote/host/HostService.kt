package org.dx340.remote.host

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.*
import org.dx340.remote.ble.BlePeripheral
import org.dx340.remote.protocol.CommandLedger
import org.dx340.remote.protocol.RoutingPolicy
import org.dx340.remote.protocol.JsonEnvelopeGuard
import org.dx340.remote.protocol.MutationGate
import org.dx340.remote.sources.SourceRegistry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.UUID

@SuppressLint("MissingPermission")
class HostService : Service() {
    companion object { var instance: HostService? = null; private set }
    private val main = Handler(Looper.getMainLooper())
    val epoch = UUID.randomUUID().toString()
    lateinit var sources: SourceRegistry; private set
    private lateinit var ble: BlePeripheral
    private val logs = ArrayDeque<String>()
    private val approved get() = getSharedPreferences("controllers", MODE_PRIVATE)
    private val ledgers = mutableMapOf<String, CommandLedger>()
    private val mutationGate = MutationGate()
    private var authenticated: BluetoothDevice? = null
    var pending: BluetoothDevice? = null; private set
    var status = "Starting"; private set
    var observer: (() -> Unit)? = null
    private var started = false
    private val bonds = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF || intent.action == Intent.ACTION_USER_PRESENT) { sources.externalStateChanged(); return }
            if (intent.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED || intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) != BluetoothDevice.BOND_NONE) return
            val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device != null && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                approved.edit().remove(device.address).apply()
                if (authenticated == device) { authenticated = null; ble.disconnect(device) }
                record("OS bond removed; associated approval revoked")
            }
        }
    }
    private var requestsInWindow = 0
    private var windowStart = 0L
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate(); instance = this
        sources = SourceRegistry(this, ComponentName(this, SessionListener::class.java), AppleUiAdapterProvider(this)) { onSourceChanged() }
        ble = BlePeripheral(this, ::record, { approved.getBoolean(it.address, false) }, { device -> if (authenticated == device) authenticated = null; if (pending == device) pending = null }, ::receive)
        val events = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED).apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(bonds, events, Context.RECEIVER_EXPORTED) else registerReceiver(bonds, events)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) { stopSelf(); return START_NOT_STICKY }
        if (!started) {
            val bonded = getSystemService(BluetoothManager::class.java).adapter?.bondedDevices?.map { it.address }?.toSet() ?: emptySet()
            approved.edit().apply { approved.all.keys.filter { it !in bonded }.forEach { remove(it) } }.apply()
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("ble", "Bluetooth remote", NotificationManager.IMPORTANCE_LOW))
            val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val stop = PendingIntent.getService(this, 1, Intent(this, HostService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
            startForeground(340, Notification.Builder(this, "ble").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("DX340 remote enabled").setContentText("Music continues in your player").setContentIntent(open)
                .addAction(Notification.Action.Builder(null, "Stop remote", stop).build()).setOngoing(true).build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            started = true
            sources.start()
            runCatching { ble.start() }.onFailure { record("BLE_START_FAILED:${it.message}") }
        }
        when (intent?.action) { "PAIR" -> openPairing(); "PROBE" -> { sources.refresh(); sources.probeBrowsers(); export() } }
        return START_NOT_STICKY
    }
    fun openPairing() { ble.pairingUntil = SystemClock.elapsedRealtime() + 60000; record("PAIRING_WINDOW:60 seconds; approve OS pairing and controller locally") }
    fun approve() {
        val device = pending ?: return
        if (SystemClock.elapsedRealtime() > ble.pairingUntil || device.bondState != BluetoothDevice.BOND_BONDED) { record("APPROVAL_DENIED: reopen pairing and establish authenticated OS bond"); return }
        if (!approved.contains(device.address) && approved.all.size >= 8) { record("CONTROLLER_LIMIT: revoke an unused controller"); return }
        approved.edit().putBoolean(device.address, true).apply(); pending = null; record("CONTROLLER_APPROVED: reconnect or retry authorization")
    }
    fun revokeAll() { approved.edit().clear().apply(); authenticated?.let { ble.disconnect(it) }; authenticated = null; pending = null; record("All controller approvals revoked; OS bonds retained") }
    private fun record(text: String) {
        status = text; if (logs.size >= 200) logs.removeFirst()
        logs.addLast("${System.currentTimeMillis()} $text"); android.util.Log.i("DX340Remote", text)
        export(); observer?.invoke()
    }
    private fun export() {
        val report = sources.diagnostic().put("hostEpoch", epoch).put("bleStatus", status).put("events", JSONArray(logs.toList()))
        File(filesDir, "diagnostic.json").writeText(report.toString(2))
    }
    fun diagnostic(): String { export(); return File(filesDir, "diagnostic.json").readText() }
    private fun reply(device: BluetoothDevice, request: JSONObject, type: String, data: JSONObject = JSONObject()): String {
        val result = JSONObject().put("v", 1).put("id", request.optString("id")).put("hostEpoch", epoch).put("type", type).put("data", data).toString()
        val safeResult = if (result.toByteArray(Charsets.UTF_8).size <= 65536) result else JSONObject().put("v", 1).put("id", request.optString("id")).put("hostEpoch", epoch).put("type", "ERROR").put("data", JSONObject().put("code", "RESPONSE_TOO_LARGE")).toString()
        ble.send(device, safeResult.toByteArray(Charsets.UTF_8), request.optString("type") in listOf("GET_ARTWORK", "BROWSE", "GET_QUEUE")); return safeResult
    }
    private fun onSourceChanged() {
        if (!::sources.isInitialized) return
        export(); observer?.invoke()
        authenticated?.let { device ->
            if (device.bondState != BluetoothDevice.BOND_BONDED || !approved.getBoolean(device.address, false)) { authenticated = null; ble.disconnect(device); return }
            // Coalesce metadata/state changes; continuous progress is intentionally not transmitted.
            main.removeCallbacks(pushState); main.postDelayed(pushState, 150)
        }
    }
    private val pushState = Runnable { authenticated?.let { reply(it, JSONObject(), "STATE_CHANGED", sources.state()) } }
    private fun receive(device: BluetoothDevice, bytes: ByteArray) {
        var request = JSONObject()
        var ledger: CommandLedger? = null
        var reserved = false
        var gateEntered = false
        try {
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            JsonEnvelopeGuard.validate(text)
            request = JSONObject(text)
            require(request.getInt("v") == 1) { "INCOMPATIBLE_PROTOCOL" }
            val id = request.getString("id"); require(id.matches(Regex("[A-Za-z0-9-]{1,64}"))) { "MALFORMED_MESSAGE" }
            val op = request.getString("type")
            val now = SystemClock.elapsedRealtime()
            if (now - windowStart > 1000) { requestsInWindow = 0; windowStart = now }
            require(++requestsInWindow <= 20) { "RATE_LIMIT" }
            require(device.bondState == BluetoothDevice.BOND_BONDED) { "AUTHORIZATION_DENIED" }
            if (op == "HELLO") { reply(device, request, "HELLO", JSONObject().put("maxMessage", 65536).put("pairingSecurity", "OS authenticated bond; physical verification pending")); return }
            if (op == "AUTHORIZE") {
                if (!approved.getBoolean(device.address, false)) {
                    if (now <= ble.pairingUntil) { pending = device; record("NEW_CONTROLLER: approve on DX340") }
                    error("LOCAL_APPROVAL_REQUIRED")
                }
                require(authenticated == null || authenticated == device) { "CONTROLLER_BUSY" }
                authenticated = device
                reply(device, request, "RESULT", JSONObject().put("authorized", true).put("state", sources.state()).put("sources", sources.sources())); return
            }
            require(authenticated == device && approved.getBoolean(device.address, false)) { "AUTHORIZATION_DENIED" }
            require(request.getString("hostEpoch") == epoch) { "STALE_HOST" }
            if (!ledgers.containsKey(device.address)) { require(ledgers.size < 8) { "SESSION_LIMIT" }; ledgers[device.address] = CommandLedger() }
            ledger = ledgers.getValue(device.address)
            ledger.begin(id, text)?.let { previous ->
                if (previous == "PENDING") reply(device, request, "ACCEPTED", JSONObject().put("outcome", "pending")) else ble.send(device, previous.toByteArray())
                return
            }
            reserved = true
            if (op in setOf("SELECT_SOURCE", "SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "PLAY_ITEM", "PLAY_QUEUE_ITEM", "ADJUST_VOLUME", "SET_SHUFFLE", "SET_REPEAT")) { mutationGate.enter(id); gateEntered = true }
            val args = request.optJSONObject("args") ?: JSONObject()
            fun result(data: JSONObject) { val value = reply(device, request, "RESULT", data); ledger.finish(id, value); if (gateEntered) { mutationGate.leave(id); gateEntered = false } }
            when (op) {
                "LIST_SOURCES", "GET_CAPABILITIES" -> result(JSONObject().put("sources", sources.sources()))
                "GET_STATE", "SUBSCRIBE_STATE" -> result(JSONObject().put("state", sources.state()))
                "SELECT_SOURCE" -> { sources.select(args.getString("source")); result(JSONObject().put("state", sources.state())) }
                else -> {
                    RoutingPolicy.validate(epoch, request.getString("hostEpoch"), sources.selected, request.getString("source"), sources.epoch(sources.selected), request.getString("sourceEpoch"), true)
                    when (op) {
                        "BROWSE" -> {
                            reply(device, request, "ACCEPTED")
                            sources.browse(if (args.isNull("parent")) null else args.optString("parent").takeIf { it.isNotEmpty() }, args.optInt("page", 0), if (args.isNull("revision")) null else args.optString("revision")) { data, error ->
                                val value = reply(device, request, if (error == null) "RESULT" else "ERROR", data ?: JSONObject().put("code", error)); ledger.finish(id, value)
                            }
                        }
                        "GET_QUEUE" -> result(sources.queueState())
                        "GET_ARTWORK" -> result(sources.getArtwork(args.getString("key")))
                        "PLAY_ITEM" -> {
                            reply(device, request, "ACCEPTED")
                            sources.playItem(args, { authenticated == device && approved.getBoolean(device.address, false) && device.bondState == BluetoothDevice.BOND_BONDED }) { error ->
                                val value = reply(device, request, if (error == null) "RESULT" else "ERROR", if (error == null) JSONObject().put("outcome", "dispatched") else JSONObject().put("code", error))
                                ledger.finish(id, value)
                                mutationGate.leave(id); gateEntered = false
                            }
                        }
                        "SET_PLAYING" -> {
                            reply(device, request, "ACCEPTED")
                            sources.setPlaying(args.getBoolean("playing"), stillAuthorized = { authenticated == device && approved.getBoolean(device.address, false) && device.bondState == BluetoothDevice.BOND_BONDED }) { error ->
                                val value = reply(device, request, if (error == null) "RESULT" else "ERROR", if (error == null) JSONObject().put("outcome", "observed").put("state", sources.state()) else JSONObject().put("code", error))
                                ledger.finish(id, value); mutationGate.leave(id); gateEntered = false
                            }
                        }
                        "NEXT", "PREVIOUS", "SEEK", "PLAY_QUEUE_ITEM" -> {
                            sources.command(op, args)
                            result(JSONObject().put("outcome", "dispatched").put("observation", "Await source state; audible completion is not confirmed"))
                        }
                        else -> error("UNSUPPORTED_CAPABILITY")
                    }
                }
            }
        } catch (e: Exception) {
            val code = e.message?.takeIf { it.matches(Regex("[A-Z_]{3,64}")) } ?: "MALFORMED_MESSAGE"
            val value = reply(device, request, "ERROR", JSONObject().put("code", code))
            if (reserved) ledger?.finish(request.optString("id"), value)
            if (gateEntered) mutationGate.leave(request.optString("id"))
            record("REQUEST_ERROR:$code")
        }
    }
    override fun onDestroy() { main.removeCallbacksAndMessages(null); unregisterReceiver(bonds); ble.stop(); sources.close(); observer = null; instance = null; super.onDestroy() }
}

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
import org.dx340.remote.protocol.ReplyAccess
import org.dx340.remote.protocol.AuthorizationSession
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
    private val approved by lazy { ControllerApprovalStore(this) }
    private val authorization = AuthorizationSession()
    private val ledgers = mutableMapOf<String, CommandLedger>()
    private val controlGrants = mutableMapOf<String, MutableMap<String, AuthorizationSession.Lease>>()
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
                revokeController(device.address, "OS bond removed; associated approval revoked")
            }
        }
    }
    private data class RequestWindow(var start: Long, var count: Int = 0)
    private val requestWindows = mutableMapOf<String, RequestWindow>()
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate(); instance = this
        sources = SourceRegistry(this, ComponentName(this, SessionListener::class.java), AppleUiAdapterProvider(this), MangoUiAdapterProvider(this)) { onSourceChanged() }
        ble = BlePeripheral(this, ::record, { approved.contains(it.address) }, { device ->
            authorization.revoke(device.address)
            if (authenticated == device) authenticated = null
            if (pending == device) pending = null
            requestWindows.remove(device.address)
            observer?.invoke()
        }, ::receive)
        val events = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED).apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(bonds, events, Context.RECEIVER_EXPORTED) else registerReceiver(bonds, events)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) { stopSelf(); return START_NOT_STICKY }
        if (!started) {
            val bonded = runCatching {
                getSystemService(BluetoothManager::class.java).adapter?.takeIf { it.isEnabled }?.bondedDevices?.map { it.address }?.toSet()
            }.getOrNull()
            approved.retainBonded(bonded)
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
    val pairingRemainingMs get() = (ble.pairingUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0)
    val activeControllerAddress get() = authenticated?.address?.takeIf { authorization.current(it) != null }
    fun approve(expectedAddress: String) {
        val device = pending ?: return
        if (device.address != expectedAddress) { record("APPROVAL_CHANGED: review the current pending controller"); return }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || pairingRemainingMs == 0L || device.bondState != BluetoothDevice.BOND_BONDED) { record("APPROVAL_DENIED: reopen pairing and establish authenticated OS bond"); return }
        try { approved.approve(device.address) }
        catch (e: IllegalStateException) { record(e.message ?: "CONTROLLER_LIMIT"); return }
        pending = null; record("CONTROLLER_APPROVED: reconnect or retry authorization")
    }
    fun revokeController(address: String, reason: String = "Controller approval revoked; OS bond retained") {
        authorization.revoke(address)
        if (authenticated?.address == address) { authenticated = null; main.removeCallbacks(pushState) }
        if (pending?.address == address) pending = null
        approved.revoke(address)
        // Keep the request ledger across reapproval so an old control ID cannot execute twice.
        ble.disconnectController(address)
        record(reason)
    }
    fun revokeAll() {
        authorization.clear(); authenticated = null; pending = null; main.removeCallbacks(pushState)
        approved.revokeAll(); ble.pairingUntil = 0; ble.disconnectCurrent()
        record("All controller approvals revoked; pairing closed; OS bonds retained")
    }
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
    private fun reply(device: BluetoothDevice, request: JSONObject, type: String, data: JSONObject = JSONObject(), requiredLease: AuthorizationSession.Lease? = null): String {
        val result = JSONObject().put("v", 1).put("id", request.optString("id")).put("hostEpoch", epoch).put("type", type).put("data", data).toString()
        val safeResult = if (result.toByteArray(Charsets.UTF_8).size <= 65536) result else JSONObject().put("v", 1).put("id", request.optString("id")).put("hostEpoch", epoch).put("type", "ERROR").put("data", JSONObject().put("code", "RESPONSE_TOO_LARGE")).toString()
        val bonded = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
        if ((requiredLease == null || authorization.isCurrent(requiredLease)) && ReplyAccess.maySend(type, bonded,
                approved.contains(device.address), authenticated == device && authorization.current(device.address) != null)) {
            ble.send(device, safeResult.toByteArray(Charsets.UTF_8), request.optString("type") in listOf("GET_ARTWORK", "BROWSE", "GET_QUEUE"))
        }
        return safeResult
    }
    private fun onSourceChanged() {
        if (!::sources.isInitialized) return
        export(); observer?.invoke()
        authenticated?.let { device ->
            if (device.bondState != BluetoothDevice.BOND_BONDED || !approved.contains(device.address) || authorization.current(device.address) == null) { authorization.revoke(device.address); authenticated = null; ble.disconnect(device); return }
            // Coalesce metadata/state changes; continuous progress is intentionally not transmitted.
            main.removeCallbacks(pushState); main.postDelayed(pushState, 150)
        }
    }
    private val pushState = Runnable { authenticated?.let { reply(it, JSONObject(), "STATE_CHANGED", sources.state()) } }
    private fun receive(device: BluetoothDevice, bytes: ByteArray) {
        var request = JSONObject()
        var ledger: CommandLedger? = null
        var reserved = false
        var gateLease: MutationGate.Lease? = null
        fun releaseGate() { val previous = gateLease; gateLease = null; mutationGate.leave(previous) }
        var requestLease: AuthorizationSession.Lease? = null
        fun respond(type: String, data: JSONObject = JSONObject()) = reply(device = device, request = request, type = type, data = data, requiredLease = requestLease)
        try {
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            JsonEnvelopeGuard.validate(text)
            request = JSONObject(text)
            require(request.getInt("v") == 1) { "INCOMPATIBLE_PROTOCOL" }
            val id = request.getString("id"); require(id.matches(Regex("[A-Za-z0-9-]{1,64}"))) { "MALFORMED_MESSAGE" }
            val op = request.getString("type")
            val now = SystemClock.elapsedRealtime()
            val window = requestWindows.getOrPut(device.address) { RequestWindow(now) }
            if (now - window.start > 1000) { window.count = 0; window.start = now }
            require(++window.count <= 20) { "RATE_LIMIT" }
            require(device.bondState == BluetoothDevice.BOND_BONDED) { "AUTHORIZATION_DENIED" }
            if (op == "HELLO") { respond("HELLO", JSONObject().put("maxMessage", 65536).put("pairingSecurity", "OS authenticated bond; physical verification pending")); return }
            if (op == "AUTHORIZE") {
                if (!approved.contains(device.address)) {
                    if (now <= ble.pairingUntil) { pending = device; record("NEW_CONTROLLER: approve on DX340") }
                    error("LOCAL_APPROVAL_REQUIRED")
                }
                if (authenticated != null && authenticated != device) {
                    respond("ERROR", JSONObject().put("code", "CONTROLLER_BUSY").put("ownershipRevision", authorization.ownershipRevision()))
                    return
                }
                requestLease = authorization.authorize(device.address)
                authenticated = device
                ble.setControlling(device, true)
                record("CONTROLLER_CONNECTED: approved controller; playback unchanged")
                respond("RESULT", JSONObject().put("authorized", true).put("state", sources.state()).put("sources", sources.sources())); return
            }
            if (op == "REQUEST_CONTROL") {
                require(approved.contains(device.address)) { "LOCAL_APPROVAL_REQUIRED" }
                require(request.getString("hostEpoch") == epoch) { "STALE_HOST" }
                if (!ledgers.containsKey(device.address)) { require(ledgers.size < 8) { "SESSION_LIMIT" }; ledgers[device.address] = CommandLedger() }
                ledger = ledgers.getValue(device.address)
                ledger.begin(id, text)?.let { previous ->
                    val grant = controlGrants[device.address]?.get(id)
                    when {
                        previous == "PENDING" -> respond("ACCEPTED")
                        grant != null && !authorization.isCurrent(grant) -> respond("ERROR", JSONObject().put("code", "CONTROL_REQUEST_USED"))
                        else -> ble.send(device, previous.toByteArray(Charsets.UTF_8))
                    }
                    return
                }
                reserved = true
                val previous = authenticated
                val grant = authorization.takeControl(device.address, request.getJSONObject("args").getString("ownershipRevision"))
                requestLease = grant; authenticated = device
                controlGrants.getOrPut(device.address) { mutableMapOf() }[id] = grant
                ble.setControlling(device, true)
                if (previous != null && previous != device) {
                    // Invalidate old async work before discarding unsent metadata.
                    // An already submitted indication may still finish.
                    ble.discardQueued(previous); ble.setControlling(previous, false)
                    reply(previous, JSONObject(), "CONTROL_CHANGED", JSONObject().put("code", "CONTROL_MOVED"))
                }
                val value = respond("RESULT", JSONObject().put("authorized", true).put("state", sources.state()).put("sources", sources.sources()))
                ledger.finish(id, value)
                record("CONTROL_TRANSFERRED: approved controller accepted; playback unchanged")
                return
            }
            requestLease = authorization.current(device.address)
            require(requestLease != null && authenticated == device && approved.contains(device.address)) { "AUTHORIZATION_DENIED" }
            require(request.getString("hostEpoch") == epoch) { "STALE_HOST" }
            if (!ledgers.containsKey(device.address)) { require(ledgers.size < 8) { "SESSION_LIMIT" }; ledgers[device.address] = CommandLedger() }
            ledger = ledgers.getValue(device.address)
            ledger.begin(id, text)?.let { previous ->
                if (previous == "PENDING") respond("ACCEPTED", JSONObject().put("outcome", "pending")) else ble.send(device, previous.toByteArray())
                return
            }
            reserved = true
            if (op in setOf("SELECT_SOURCE", "SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "PLAY_ITEM", "PLAY_QUEUE_ITEM", "ADJUST_VOLUME", "SET_SHUFFLE", "SET_REPEAT")) { gateLease = mutationGate.enter(id) }
            val args = request.optJSONObject("args") ?: JSONObject()
            fun result(data: JSONObject) { val value = respond("RESULT", data); ledger.finish(id, value); releaseGate() }
            when (op) {
                "LIST_SOURCES", "GET_CAPABILITIES" -> result(JSONObject().put("sources", sources.sources()))
                "GET_STATE", "SUBSCRIBE_STATE" -> {
                    // Explicit refresh can recover permission/observer state without restarting
                    // the companion or replaying any playback operation.
                    sources.refresh()
                    result(JSONObject().put("state", sources.state()))
                }
                "SELECT_SOURCE" -> { sources.select(args.getString("source")); result(JSONObject().put("state", sources.state())) }
                else -> {
                    RoutingPolicy.validate(epoch, request.getString("hostEpoch"), sources.selected, request.getString("source"), sources.epoch(sources.selected), request.getString("sourceEpoch"), true)
                    when (op) {
                        "BROWSE" -> {
                            respond("ACCEPTED")
                            sources.browseAuthorized(if (args.isNull("parent")) null else args.optString("parent").takeIf { it.isNotEmpty() }, args.optInt("page", 0), if (args.isNull("revision")) null else args.optString("revision"), { authorization.isCurrent(requestLease) && authenticated == device && approved.contains(device.address) && device.bondState == BluetoothDevice.BOND_BONDED }) { data, error ->
                                val value = respond(if (error == null) "RESULT" else "ERROR", data ?: JSONObject().put("code", error)); ledger.finish(id, value)
                            }
                        }
                        "GET_QUEUE" -> result(sources.queueState(args))
                        "GET_ARTWORK" -> result(sources.getArtwork(args.getString("key")))
                        "PLAY_ITEM" -> {
                            respond("ACCEPTED")
                            sources.playItem(args, { authorization.isCurrent(requestLease) && authenticated == device && approved.contains(device.address) && device.bondState == BluetoothDevice.BOND_BONDED }) { error ->
                                val value = respond(if (error == null) "RESULT" else "ERROR", if (error == null) JSONObject().put("outcome", "dispatched") else JSONObject().put("code", error))
                                ledger.finish(id, value)
                                releaseGate()
                            }
                        }
                        "SET_PLAYING" -> {
                            respond("ACCEPTED")
                            sources.setPlaying(args.getBoolean("playing"), stillAuthorized = { authorization.isCurrent(requestLease) && authenticated == device && approved.contains(device.address) && device.bondState == BluetoothDevice.BOND_BONDED }) { error ->
                                val value = respond(if (error == null) "RESULT" else "ERROR", if (error == null) JSONObject().put("outcome", "observed").put("state", sources.state()) else JSONObject().put("code", error))
                                ledger.finish(id, value); releaseGate()
                            }
                        }
                        "PLAY_QUEUE_ITEM" -> {
                            respond("ACCEPTED")
                            sources.playQueueItem(args, { authorization.isCurrent(requestLease) && authenticated == device && approved.contains(device.address) && device.bondState == BluetoothDevice.BOND_BONDED }) { error ->
                                val value = respond(if (error == null) "RESULT" else "ERROR", if (error == null) JSONObject().put("outcome", "dispatched") else JSONObject().put("code", error))
                                ledger.finish(id, value); releaseGate()
                            }
                        }
                        "NEXT", "PREVIOUS", "SEEK" -> {
                            sources.command(op, args)
                            result(JSONObject().put("outcome", "dispatched").put("observation", "Await source state; audible completion is not confirmed"))
                        }
                        "ADJUST_VOLUME" -> {
                            respond("ACCEPTED")
                            sources.outputVolume.control.adjust(args.opt("delta"), args.opt("expectedVolume"), args.optString("profileId"), {
                                authorization.isCurrent(requestLease) && authenticated == device && approved.contains(device.address) && device.bondState == BluetoothDevice.BOND_BONDED
                            }) { error ->
                                val data = if (error == null) JSONObject().put("outcome", "observed") else JSONObject().put("code", error)
                                val value = respond(if (error == null) "RESULT" else "ERROR", data.put("state", sources.state()))
                                ledger.finish(id, value); releaseGate()
                            }
                        }
                        else -> error("UNSUPPORTED_CAPABILITY")
                    }
                }
            }
        } catch (e: Exception) {
            val code = e.message?.takeIf { it.matches(Regex("[A-Z_]{3,64}")) } ?: "MALFORMED_MESSAGE"
            val value = respond("ERROR", JSONObject().put("code", code))
            if (reserved) ledger?.finish(request.optString("id"), value)
            releaseGate()
            record("REQUEST_ERROR:$code")
        }
    }
    override fun onDestroy() { authorization.clear(); authenticated = null; pending = null; main.removeCallbacksAndMessages(null); unregisterReceiver(bonds); ble.stop(); sources.close(); observer = null; instance = null; super.onDestroy() }
}

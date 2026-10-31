package org.dx340.remote.controller

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import org.dx340.remote.ble.CentralPhase
import org.dx340.remote.protocol.PlayerDiscovery
import org.dx340.remote.protocol.ApprovalRetry
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import org.dx340.remote.ble.BleCentral
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import org.dx340.remote.protocol.RequestJournal
import org.dx340.remote.protocol.ControllerSession
import org.dx340.remote.protocol.ControlProposal
import org.dx340.remote.protocol.ReplyBudget
import org.dx340.remote.protocol.BrowseContext
import org.dx340.remote.protocol.BrowseContextTransition
import org.dx340.remote.protocol.BrowseReset
import org.dx340.remote.protocol.ConnectionSetupDeadline
import org.dx340.remote.protocol.AssistedPageUse
import org.dx340.remote.protocol.HostReplyGate
import org.dx340.remote.protocol.HostReplyDecision

private const val DISPATCHED_STATUS = "Playback request sent."

@SuppressLint("MissingPermission")
class RemoteModel(private val context: Context) {
    var observer: (() -> Unit)? = null
    internal var connectionObserver: (() -> Unit)? = null
    private fun update() { connectionObserver?.invoke(); observer?.invoke() }
    private val cacheDir = java.io.File(context.cacheDir, "dx340").apply { mkdirs() }
    private val snapshotFile = java.io.File(context.cacheDir, "dx340-metadata.json")
    var status = "Enable Bluetooth, then find your DX340."; private set
    private val session = ControllerSession()
    private val journal = RequestJournal { android.os.SystemClock.elapsedRealtime() }
    private val diagnosticSessionId = UUID.randomUUID().toString()
    private val controlProposal = ControlProposal()
    val canRequestControl get() = !authorized && controlProposal.available && pending.values.none { it == "REQUEST_CONTROL" }
    val authorized get() = session.authorized && bluetoothAccess() && bluetoothOn()
    var state = JSONObject(); private set
    var stateSampleId = ""; private set
    private var stateReceivedAt: Long? = null
    val stateAgeMs get() = stateReceivedAt?.let { (android.os.SystemClock.elapsedRealtime() - it).coerceAtLeast(0) }
    var sources = JSONArray(); private set
    var library = JSONObject(); private set
    var queue = JSONArray(); private set
    var queueRevision = ""; private set
    var queuePage = JSONObject(); private set
    val queueLoading get() = activeQueueId?.let { pending.containsKey(it) } == true
    val epoch get() = session.hostEpoch
    var artworkBytes: ByteArray? = null; private set
    private val main = Handler(Looper.getMainLooper())
    private val pending get() = session.pending
    private val mutations = setOf("SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "SELECT_SOURCE", "PLAY_ITEM", "PLAY_QUEUE_ITEM", "ADJUST_VOLUME")
    val mutationPending get() = pending.values.any { it in mutations }
    private data class BrowseLocation(val source: String, val epoch: String, val parent: String?)
    private val browseLocations = mutableMapOf<String, BrowseLocation>()
    private var activeBrowseId: String? = null
    private data class QueueLocation(val source: String, val epoch: String, val offset: Int, val revision: String?)
    private val queueLocations = mutableMapOf<String, QueueLocation>()
    private var activeQueueId: String? = null
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val connectionPreferences = context.getSharedPreferences("approved-player", Context.MODE_PRIVATE)
    private var connectionPhase = "idle"
        set(value) { field = value; journal.phase(value) }
    private var discoveryGeneration = 0L
    private val approvalRetry = ApprovalRetry()
    private var approvalCheck: Runnable? = null
    private val setupDeadline = ConnectionSetupDeadline()
    private val assistedPageUse = AssistedPageUse()
    private var setupTimeout: Runnable? = null
    val found get() = devices.values.toList()
    private val ble: BleCentral = BleCentral(context, { status = it; update() }, { devices[it.address] = it; update() }, { beginSetupDeadline(); send("HELLO", bootstrap = true) }, ::receive, phase = ::centralPhaseChanged) {
        cancelSetupDeadline(); cancelApprovalCheck()
        abandonAssistedPage(); journal.abandon(); session.lost(); controlProposal.clear(); browseLocations.clear(); queueLocations.clear(); activeBrowseId = null; activeQueueId = null; status = "Disconnected. Cached lists may be stale; reconnect to control."; update()
    }
    init {
        journal.phase("idle")
        runCatching {
            if (snapshotFile.length() in 1..262144) {
                val cached = JSONObject(snapshotFile.readText())
                state = cached.optJSONObject("state") ?: JSONObject(); library = cached.optJSONObject("library") ?: JSONObject(); sources = cached.optJSONArray("sources") ?: JSONArray()
                status = "Cached metadata is stale. Connect to DX340 before controlling playback."
            }
        }
    }
    private fun bluetoothAccess() = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT).all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    private fun bluetoothOn() = bluetoothAccess() && runCatching { context.getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true }.getOrDefault(false)
    val bluetoothReady get() = bluetoothOn()
    /** Keep foreground ownership for a live link or bounded connection work only. */
    internal val needsConnectionService get() = bluetoothReady && (authorized ||
        connectionPhase in setOf("searching", "connecting", "approval", "busy"))
    val connectionSetup: JSONObject get() = JSONObject()
        .put("phase", when { !bluetoothAccess() -> "permission"; !bluetoothOn() -> "bluetoothOff"; authorized -> "connected"; canRequestControl -> "busy"; else -> connectionPhase })
        .put("savedName", connectionPreferences.getString("name", "Player"))
        .put("hasSavedPlayer", !connectionPreferences.getString("id", null).isNullOrEmpty())
    private fun centralPhaseChanged(value: CentralPhase) {
        connectionPhase = when (value) {
            CentralPhase.SEARCHING -> "searching"
            CentralPhase.CONNECTING, CentralPhase.CHECKING, CentralPhase.RETRYING -> "connecting"
            CentralPhase.FAILED -> "failed"
            CentralPhase.IDLE -> "idle"
        }
        update()
    }
    fun scan() = discoverPlayers(automatic = false)
    fun searchPlayers() { disconnect(); discoverPlayers(automatic = false) }
    fun connectPlayer() {
        if (authorized || connectionPhase in setOf("searching", "connecting", "approval")) return
        disconnect()
        if (!bluetoothOn()) { status = "Enable Bluetooth and allow Nearby Devices access to connect."; update(); return }
        val id = connectionPreferences.getString("id", null)
        val remembered = id?.let { runCatching { context.getSystemService(BluetoothManager::class.java).adapter?.getRemoteDevice(it) }.getOrNull() }
        if (remembered?.bondState == BluetoothDevice.BOND_BONDED) {
            devices[remembered.address] = remembered; connect(remembered)
        } else discoverPlayers(automatic = true)
    }
    private fun discoverPlayers(automatic: Boolean) {
        cancelApprovalCheck(); val generation = ++discoveryGeneration
        devices.clear()
        runCatching {
            ble.scan { success ->
                if (generation != discoveryGeneration) return@scan
                if (!success) { connectionPhase = "failed"; update(); return@scan }
                val choice = PlayerDiscovery.finish(devices.keys, connectionPreferences.getString("id", null))
                val target = choice.target
                if (automatic && target != null) connect(devices.getValue(target))
                else {
                    connectionPhase = if (devices.isEmpty()) "failed" else "choosing"
                    status = if (devices.isEmpty()) "No compatible player found. Start Companion on your player, keep it nearby, then try again."
                        else "Choose the player you want to control."
                    update()
                }
            }
        }.onFailure { connectionPhase = "failed"; status = it.message ?: "Bluetooth unavailable" }
        update()
    }
    fun connect(device: BluetoothDevice) {
        discoveryGeneration++; ble.stopScan(); cancelApprovalCheck(); ble.connect(device)
    }
    private fun rememberApprovedPlayer() {
        val device = ble.connectedDevice ?: return
        val name = runCatching { device.name }.getOrNull()?.replace(Regex("[\\p{Cntrl}\u202A-\u202E\u2066-\u2069]"), "")?.trim()?.take(60)?.takeIf { it.isNotEmpty() } ?: "Player"
        connectionPreferences.edit().putString("id", device.address).putString("name", name).apply()
    }
    private fun cancelApprovalCheck() {
        approvalCheck?.let(main::removeCallbacks); approvalCheck = null; approvalRetry.clear()
    }
    private fun awaitLocalApproval() {
        val ticket = approvalRetry.begin()
        if (approvalCheck != null) return
        fun schedule() {
            approvalCheck = Runnable {
                approvalCheck = null
                if (!approvalRetry.current(ticket)) return@Runnable
                if (approvalRetry.mayCheck(ticket, setupDeadline.active, authorized, pending.values.any { it == "AUTHORIZE" })) authorize()
                if (approvalRetry.current(ticket)) schedule()
            }.also { main.postDelayed(it, 1500) }
        }
        schedule()
    }
    fun disconnect() { discoveryGeneration++; ble.stopScan(); cancelApprovalCheck(); abandonAssistedPage(); journal.abandon(); session.lost(); controlProposal.clear(); browseLocations.clear(); queueLocations.clear(); activeBrowseId = null; activeQueueId = null; ble.disconnect(); status = "Disconnected. Cached lists may be stale; reconnect to control."; update() }
    internal fun bluetoothAvailabilityChanged() {
        // Adapter shutdown need not deliver a GATT connection-loss callback.
        // Clear the old session/retries now; enabling the radio only refreshes
        // readiness and never replays a command or grants authorization.
        if (!bluetoothReady) disconnect() else update()
    }
    fun refreshOnForeground() { if (!bluetoothAccess() || !bluetoothOn()) { disconnect(); status = "Enable Bluetooth and allow access to connect."; update(); return }; if (expireSetupIfNeeded()) return; if (authorized) send("GET_STATE") else update() }
    fun close() { ble.stopScan(); ble.disconnect(); main.removeCallbacksAndMessages(null) }
    fun authorize() = send("AUTHORIZE", bootstrap = true)
    private fun cancelSetupDeadline() {
        setupTimeout?.let(main::removeCallbacks); setupTimeout = null; setupDeadline.clear()
    }
    private fun beginSetupDeadline() {
        if (setupDeadline.active) return
        val ticket = setupDeadline.begin(android.os.SystemClock.elapsedRealtime())
        setupTimeout = Runnable { expireSetupIfNeeded(ticket) }.also { main.postDelayed(it, ConnectionSetupDeadline.TIMEOUT_MS) }
    }
    private fun expireSetupIfNeeded(ticket: Long? = null): Boolean {
        if (authorized || !setupDeadline.expired(android.os.SystemClock.elapsedRealtime(), ticket)) return false
        // Close unfinished setup without changing saved approvals or replaying a command.
        ble.disconnect(); connectionPhase = "failed"; status = ConnectionSetupDeadline.MESSAGE; update()
        return true
    }
    fun requestControl() {
        if (!canRequestControl) return
        val revision = controlProposal.consume() ?: return
        status = "Requesting control on this phone…"
        send("REQUEST_CONTROL", JSONObject().put("ownershipRevision", revision), bootstrap = true)
    }
    fun send(type: String, args: JSONObject = JSONObject(), bootstrap: Boolean = false) {
        if (expireSetupIfNeeded()) return
        if (!bootstrap && !authorized) { status = "Reconnect and authorize before controlling playback."; update(); return }
        if (library.optBoolean("invalidated") && (type == "PLAY_ITEM" || type == "BROWSE" && !args.isNull("parent"))) {
            status = "This page expired after a song selection. Browse source library again."; update(); return
        }
        if (type in mutations && mutationPending) { status = "Wait for the current playback action to finish."; update(); return }
        if (pending.size >= 8) { status = "Wait for pending commands."; update(); return }
        val id = UUID.randomUUID().toString()
        val request = JSONObject().put("v", 1).put("id", id).put("type", type).put("hostEpoch", epoch)
            .put("source", state.optString("source")).put("sourceEpoch", state.optString("sourceEpoch")).put("args", args)
        session.register(id, type)
        journal.begin(id, type, state.optString("source"))
        if (type == "PLAY_ITEM") assistedPageUse.begin(id, state.optString("source"), state.optString("sourceEpoch"),
            library.optString("revision"), AssistedPageUse.tracks(state.optString("source"), args.optString("item"),
                assistedBrowsing, args.optString("revision"), library.optString("revision")))
        if (type == "GET_QUEUE") {
            queueLocations[id] = QueueLocation(state.optString("source"), state.optString("sourceEpoch"), args.optInt("offset", 0), if (args.isNull("revision")) null else args.optString("revision"))
            activeQueueId = id
        }
        if (type == "BROWSE") {
            browseLocations[id] = BrowseLocation(state.optString("source"), state.optString("sourceEpoch"), if (args.isNull("parent")) null else args.optString("parent"))
            activeBrowseId = id; library = JSONObject().put("loading", true); status = "Loading library…"; update()
        }
        update()
        try {
            ble.send(request.toString().toByteArray())
            // Types only: no metadata, identities, arguments or credentials.
            android.util.Log.i("DX340RemoteClient", "QUEUED:$type")
        }
        catch (e: Exception) { journal.finish(id, "send_failed"); pending.remove(id); retireAssistedPage(id); browseLocations.remove(id); queueLocations.remove(id); status = e.message ?: "Send failed"; update(); return }
        main.postDelayed({ if (pending.remove(id) != null) { journal.finish(id, "timeout"); if (type == "AUTHORIZE") { cancelApprovalCheck(); connectionPhase = "failed" }; retireAssistedPage(id); browseLocations.remove(id); queueLocations.remove(id); if (id == activeBrowseId) library = JSONObject(); status = "Reply timed out; outcome unknown. Refresh state before retrying."; update() } }, ReplyBudget.milliseconds(type, args.optString("item")))
    }
    fun browse(parent: String? = null, page: Int = 0) = send("BROWSE", JSONObject().put("parent", parent ?: JSONObject.NULL).put("page", page)
        .put("revision", if (parent == null) JSONObject.NULL else library.optString("revision", state.optString("sourceEpoch"))))
    private fun retireAssistedPage(id: String) {
        if (assistedPageUse.finish(id, state.optString("source"), state.optString("sourceEpoch"), library.optString("revision"))) {
            // Keep the rendered rows in place, but their one-use source action IDs are retired.
            library.put("invalidated", true)
        }
    }
    private fun abandonAssistedPage() {
        if (assistedPageUse.abandon(state.optString("source"), state.optString("sourceEpoch"), library.optString("revision"))) {
            library.put("invalidated", true)
        }
    }
    fun readQueue(offset: Int = 0) = send("GET_QUEUE", JSONObject().put("offset", offset)
        .put("revision", if (offset == 0) JSONObject.NULL else queueRevision))
    private fun receive(bytes: ByteArray) {
        if (expireSetupIfNeeded()) return
        try {
            val reply = JSONObject(String(bytes, Charsets.UTF_8)); require(reply.getInt("v") == 1)
            val id = reply.optString("id"); val type = reply.getString("type"); val data = reply.getJSONObject("data")
            if (type == "CONTROL_CHANGED") {
                if (reply.optString("hostEpoch") == epoch && data.optString("code") == "CONTROL_MOVED") {
                    disconnect(); status = "Control moved to another approved controller. Music continues on DX340."; update()
                }
                return
            }
            val operation = pending[id]
            val route = HostReplyGate.classify(type, operation, epoch, reply.opt("hostEpoch") as? String ?: "", authorized)
            if (route == HostReplyDecision.IGNORE) return
            if (route == HostReplyDecision.RECONNECT) {
                disconnect(); connectionPhase = "failed"; status = HostReplyGate.CHANGED_MESSAGE; update(); return
            }
            // Observe only matched, current-host replies; no payload/identity fields enter the journal.
            journal.reply(id, type, data.optString("outcome"), data.optString("code"))
            val location = browseLocations[id]
            val queueLocation = queueLocations[id]
            if (type != "ACCEPTED") queueLocations.remove(id)
            if (type != "ACCEPTED" && id.isNotEmpty()) pending.remove(id)
            if (type != "ACCEPTED" && reply.optString("hostEpoch") == epoch) retireAssistedPage(id)
            if (route == HostReplyDecision.HELLO) { journal.markTerminalApplied(id); journal.abandon("session_reset"); controlProposal.clear(); session.hello(reply.getString("hostEpoch")); authorize(); return }
            if (type == "ERROR" && reply.optString("hostEpoch") == epoch && operation == "AUTHORIZE" && !authorized) {
                if (data.optString("code") == "CONTROLLER_BUSY") {
                    ble.pauseReconnection(); cancelApprovalCheck(); connectionPhase = "busy"
                    controlProposal.observeBusy(reply.optString("hostEpoch"), epoch, data.optString("ownershipRevision"))
                } else {
                    controlProposal.clear()
                    if (data.optString("code") == "LOCAL_APPROVAL_REQUIRED") {
                        ble.pauseReconnection(); connectionPhase = "approval"; awaitLocalApproval()
                    } else { cancelApprovalCheck(); connectionPhase = "failed" }
                }
            }
            if (type == "ERROR") { data.optJSONObject("state")?.takeIf { reply.optString("hostEpoch") == epoch }?.let { applyState(it, false) }; browseLocations.remove(id); if (id == activeBrowseId) library = JSONObject(); if (id == activeQueueId) { queue = JSONArray(); queueRevision = ""; queuePage = JSONObject() }; status = errorMessage(data.optString("code", "Unknown source error")); journal.markTerminalApplied(id); update(); return }
            if (type == "ACCEPTED") { status = "Waiting for source…"; update(); return }
            if (operation in listOf("AUTHORIZE", "REQUEST_CONTROL") && data.optBoolean("authorized")) { controlProposal.clear(); session.approve(reply.getString("hostEpoch")); cancelSetupDeadline(); cancelApprovalCheck(); rememberApprovedPlayer(); connectionPhase = "connected"; ble.resumeReconnection(); status = "Connected to player" }
            if (reply.optString("hostEpoch") != epoch) { disconnect(); return }
            data.optJSONObject("state")?.let { applyState(it, operation !in listOf("AUTHORIZE", "REQUEST_CONTROL")) }
            if (type == "STATE_CHANGED") applyState(data)
            if (type == "RESULT" && operation == "GET_STATE" && data.optJSONObject("state") != null && status == DISPATCHED_STATUS) {
                // This confirms a state refresh, not that the earlier command took effect.
                status = "Player state refreshed."
            }
            data.optJSONArray("sources")?.let { sources = it }
            if (operation == "BROWSE") {
                browseLocations.remove(id)
                if (id != activeBrowseId || location == null || location.source != state.optString("source") || location.epoch != state.optString("sourceEpoch")) return
                library = data.put("requestedParent", location.parent ?: JSONObject.NULL).put("scopeSource", location.source).put("scopeEpoch", location.epoch)
                status = "Library loaded. Offline availability is source dependent."
            }
            if (operation == "GET_QUEUE" && id == activeQueueId && queueLocation != null && queueLocation.source == state.optString("source") && queueLocation.epoch == state.optString("sourceEpoch")) {
                val items = data.getJSONArray("items"); val revision = data.getString("revision")
                val offset = data.optInt("offset", 0); val total = data.optInt("total", items.length())
                val next = if (data.isNull("nextOffset")) null else data.getInt("nextOffset")
                require(revision.isNotEmpty() && (queueLocation.revision == null || revision == queueLocation.revision))
                require(offset == queueLocation.offset && offset in 0..10000 && items.length() <= 50 && total in (offset + items.length())..10000)
                require(if (next == null) offset + items.length() == total else items.length() > 0 && next == offset + items.length() && next < total)
                queue = items; queueRevision = revision
                queuePage = JSONObject().put("offset", offset).put("total", total).put("nextOffset", next ?: JSONObject.NULL)
                status = "Queue loaded."
            }
            if (operation == "GET_ARTWORK") {
                val key = data.getString("key")
                val art = android.util.Base64.decode(data.getString("base64"), android.util.Base64.NO_WRAP)
                val digest = java.security.MessageDigest.getInstance("SHA-256").digest(art).joinToString("") { "%02x".format(it.toInt() and 255) }
                require(key == digest && art.size <= 49152)
                java.io.File(cacheDir, key + ".jpg").writeBytes(art)
                if (key == state.optString("artworkKey")) artworkBytes = art
                cacheDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(100)?.forEach { it.delete() }
            }
            val artKey = state.optString("artworkKey").takeIf { it.matches(Regex("[a-f0-9]{64}")) }
            if (artKey != null && operation != "GET_ARTWORK") {
                val cached = java.io.File(cacheDir, artKey + ".jpg")
                if (cached.isFile) artworkBytes = cached.readBytes()
                else { artworkBytes = null; main.postDelayed({ if (authorized && pending.isEmpty() && artKey == state.optString("artworkKey")) send("GET_ARTWORK", JSONObject().put("key", artKey)) }, 300) }
            } else if (artKey == null) artworkBytes = null
            if (data.optString("outcome") == "dispatched") { status = DISPATCHED_STATUS; send("GET_STATE") }
            if (data.optString("outcome") == "observed") status = "Playback state confirmed by source."
            val snapshot = JSONObject().put("state", state).put("sources", sources).put("library", library).toString()
            if (snapshot.toByteArray().size <= 262144) runCatching { snapshotFile.writeText(snapshot) }
            journal.markTerminalApplied(id)
            update()
        } catch (_: Exception) { disconnect(); status = "Invalid host reply; reconnect."; update() }
    }
    /** Deliberate user export only; normal snapshots never contain this journal. */
    fun diagnostics(): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return JournalReportJson.encode(journal)
            .put("modelInstanceId", diagnosticSessionId)
            .put("app", JSONObject().put("id", context.packageName).put("versionName", info.versionName).put("versionCode", info.longVersionCode))
            .put("os", JSONObject().put("version", android.os.Build.VERSION.RELEASE).put("api", android.os.Build.VERSION.SDK_INT).put("model", android.os.Build.MODEL))
            .put("platform", "android").put("authorized", authorized)
            .put("connectionPhase", connectionSetup.optString("phase"))
            .put("timingScope", "Native send start to matched decoded reply, including encoding, queued writes and reassembly; not a GATT frame ACK or acoustic measurement.")
            .put("privacy", "Current model instance only. No music metadata, arguments, Bluetooth identities or credentials. Process restart clears this history.")
            .toString(2)
    }
    private fun isAssistedReady(snapshot: JSONObject): Boolean {
        val caps = snapshot.optJSONObject("capabilities") ?: return false
        return caps.optBoolean(if (caps.optString("browseMode") == "ui-assisted") "canBrowse" else "canBrowseFolders")
    }
    val assistedBrowsing get() = library.optBoolean("uiAssisted") || state.optJSONObject("capabilities")?.optString("browseMode") == "ui-assisted"
    val assistedReady get() = authorized && isAssistedReady(state)
    private fun applyState(new: JSONObject, notifyLibraryChange: Boolean = true) {
        fun context(value: JSONObject) = BrowseContext(value.optString("source"), value.optString("sourceEpoch"),
            value.optJSONObject("capabilities")?.optString("browseMode") ?: "", isAssistedReady(value))
        val transition = BrowseContextTransition.evaluate(context(state), context(new),
            library.length() > 0 || activeBrowseId != null,
            queue.length() > 0 || queuePage.length() > 0 || queueRevision.isNotEmpty() || activeQueueId != null,
            assistedBrowsing || activeBrowseId?.let { browseLocations[it]?.parent?.startsWith("mango-ui:") } == true,
            notifyLibraryChange && authorized)
        if (transition.reset == BrowseReset.ALL) {
            library = JSONObject(); queue = JSONArray(); queueRevision = ""; queuePage = JSONObject(); artworkBytes = null; activeBrowseId = null; activeQueueId = null; browseLocations.clear(); queueLocations.clear()
        } else if (transition.reset == BrowseReset.ASSISTED) {
            // Keep native pages/queues, but never republish an assisted frame across grant/sleep changes.
            library = JSONObject(); activeBrowseId = null; browseLocations.clear()
        }
        transition.message?.let { status = it }
        state = new
        stateReceivedAt = android.os.SystemClock.elapsedRealtime()
        stateSampleId = UUID.randomUUID().toString()
    }
    private fun errorMessage(code: String) = when (code) {
        "CONTROLLER_BUSY" -> "Another approved controller has control. Open Setup to take control on this phone."
        "STALE_CONTROL", "CONTROL_REQUEST_USED" -> "Control changed. Retry authorization to check who has control."
        "STALE_VOLUME" -> "Volume changed on DX340. Refresh before choosing another step."
        "VOLUME_AT_BOUNDARY" -> "This is the edge of the locally confirmed range. Recheck volume on DX340 to use another range."
        "VOLUME_PROFILE_CHANGED", "VOLUME_VERIFICATION_REQUIRED", "VOLUME_SOURCE_NOT_READY" -> "Play the selected source and verify its output volume in the DX340 companion."
        "UI_PERMISSION_REQUIRED" -> "On DX340, enable the selected player's optional browsing service in Accessibility settings."
        "UI_SCREEN_UNLOCK_REQUIRED" -> "Wake and unlock DX340 for assisted browsing."
        "UI_FOREGROUND_REQUIRED", "UI_SETUP_REQUIRED", "UI_UNEXPECTED_SCREEN", "UI_STORAGE_CONTEXT_REQUIRED" -> "Open the selected player's local/downloaded library on DX340, then refresh."
        "UI_VERSION_UNVERIFIED" -> "The selected player changed. Assisted browsing needs revalidation."
        "UI_ACTIVE_PAGE_UNVERIFIED", "UI_TREE_CHANGED", "UI_ACTION_TIMEOUT", "UI_ACTION_INTERRUPTED" -> "The player's screen changed or did not settle. Refresh the library before retrying."
        "UI_MODE_DISABLED" -> "On DX340, enable the selected player's optional browsing mode."
        "STALE_LIBRARY", "EXPIRED_ITEM" -> "The library changed. Refresh before selecting a track."
        "UI_BUSY", "SOURCE_BUSY" -> "Wait for the current action to finish."
        "LOCAL_APPROVAL_REQUIRED" -> "On the player, open pairing and approve this phone. Connection will finish automatically."
        else -> "Action failed ($code). Refresh the player to check its state."
    }
}

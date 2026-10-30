package org.dx340.remote.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import org.dx340.remote.ble.BleCentral
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import org.dx340.remote.protocol.ControllerSession
import org.dx340.remote.protocol.ControlProposal
import org.dx340.remote.protocol.ReplyBudget

@SuppressLint("MissingPermission")
class RemoteModel(context: Context) {
    var observer: (() -> Unit)? = null
    private fun update() { observer?.invoke() }
    private val cacheDir = java.io.File(context.cacheDir, "dx340").apply { mkdirs() }
    private val snapshotFile = java.io.File(context.cacheDir, "dx340-metadata.json")
    var status = "Enable Bluetooth, then find your DX340."; private set
    private val session = ControllerSession()
    private val controlProposal = ControlProposal()
    val canRequestControl get() = !authorized && controlProposal.available && pending.values.none { it == "REQUEST_CONTROL" }
    val authorized get() = session.authorized
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
    val found get() = devices.values.toList()
    private val ble = BleCentral(context, { status = it; update() }, { devices[it.address] = it; update() }, { send("HELLO", bootstrap = true) }, ::receive) {
        session.lost(); controlProposal.clear(); browseLocations.clear(); queueLocations.clear(); activeBrowseId = null; activeQueueId = null; status = "Disconnected. Cached lists may be stale; reconnect to control."; update()
    }
    init {
        runCatching {
            if (snapshotFile.length() in 1..262144) {
                val cached = JSONObject(snapshotFile.readText())
                state = cached.optJSONObject("state") ?: JSONObject(); library = cached.optJSONObject("library") ?: JSONObject(); sources = cached.optJSONArray("sources") ?: JSONArray()
                status = "Cached metadata is stale. Connect to DX340 before controlling playback."
            }
        }
    }
    fun scan() { devices.clear(); runCatching { ble.scan() }.onFailure { status = it.message ?: "Bluetooth unavailable" }; update() }
    fun connect(device: BluetoothDevice) = ble.connect(device)
    fun disconnect() { session.lost(); controlProposal.clear(); browseLocations.clear(); queueLocations.clear(); activeBrowseId = null; activeQueueId = null; ble.disconnect(); status = "Disconnected. Cached lists may be stale; reconnect to control."; update() }
    fun refreshOnForeground() { if (authorized) send("GET_STATE") else update() }
    fun close() { ble.stopScan(); ble.disconnect(); main.removeCallbacksAndMessages(null) }
    fun authorize() = send("AUTHORIZE", bootstrap = true)
    fun requestControl() {
        if (!canRequestControl) return
        val revision = controlProposal.consume() ?: return
        status = "Requesting control on this phone…"
        send("REQUEST_CONTROL", JSONObject().put("ownershipRevision", revision), bootstrap = true)
    }
    fun send(type: String, args: JSONObject = JSONObject(), bootstrap: Boolean = false) {
        if (!bootstrap && !authorized) { status = "Reconnect and authorize before controlling playback."; update(); return }
        if (type in mutations && mutationPending) { status = "Wait for the current playback action to finish."; update(); return }
        if (pending.size >= 8) { status = "Wait for pending commands."; update(); return }
        val id = UUID.randomUUID().toString()
        val request = JSONObject().put("v", 1).put("id", id).put("type", type).put("hostEpoch", epoch)
            .put("source", state.optString("source")).put("sourceEpoch", state.optString("sourceEpoch")).put("args", args)
        session.register(id, type)
        if (type == "GET_QUEUE") {
            queueLocations[id] = QueueLocation(state.optString("source"), state.optString("sourceEpoch"), args.optInt("offset", 0), if (args.isNull("revision")) null else args.optString("revision"))
            activeQueueId = id
        }
        if (type == "BROWSE") {
            browseLocations[id] = BrowseLocation(state.optString("source"), state.optString("sourceEpoch"), if (args.isNull("parent")) null else args.optString("parent"))
            activeBrowseId = id; library = JSONObject().put("loading", true); status = "Loading library…"; update()
        }
        update()
        try { ble.send(request.toString().toByteArray()) }
        catch (e: Exception) { pending.remove(id); browseLocations.remove(id); queueLocations.remove(id); status = e.message ?: "Send failed"; update(); return }
        main.postDelayed({ if (pending.remove(id) != null) { browseLocations.remove(id); queueLocations.remove(id); if (id == activeBrowseId) library = JSONObject(); status = "Reply timed out; outcome unknown. Refresh state before retrying."; update() } }, ReplyBudget.milliseconds(type, args.optString("item")))
    }
    fun browse(parent: String? = null, page: Int = 0) = send("BROWSE", JSONObject().put("parent", parent ?: JSONObject.NULL).put("page", page)
        .put("revision", if (parent == null) JSONObject.NULL else library.optString("revision", state.optString("sourceEpoch"))))
    fun readQueue(offset: Int = 0) = send("GET_QUEUE", JSONObject().put("offset", offset)
        .put("revision", if (offset == 0) JSONObject.NULL else queueRevision))
    private fun receive(bytes: ByteArray) {
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
            if (type != "STATE_CHANGED" && operation == null) return
            val location = browseLocations[id]
            val queueLocation = queueLocations[id]
            if (type != "ACCEPTED") queueLocations.remove(id)
            if (type != "ACCEPTED" && id.isNotEmpty()) pending.remove(id)
            if (type == "HELLO") { controlProposal.clear(); session.hello(reply.getString("hostEpoch")); authorize(); return }
            if (type == "ERROR" && reply.optString("hostEpoch") == epoch && operation == "AUTHORIZE" && !authorized) {
                if (data.optString("code") == "CONTROLLER_BUSY") {
                    ble.pauseReconnection()
                    controlProposal.observeBusy(reply.optString("hostEpoch"), epoch, data.optString("ownershipRevision"))
                } else controlProposal.clear()
            }
            if (type == "ERROR") { data.optJSONObject("state")?.takeIf { reply.optString("hostEpoch") == epoch }?.let(::applyState); browseLocations.remove(id); if (id == activeBrowseId) library = JSONObject(); if (id == activeQueueId) { queue = JSONArray(); queueRevision = ""; queuePage = JSONObject() }; status = errorMessage(data.optString("code", "Unknown source error")); update(); return }
            if (type == "ACCEPTED") { status = "Waiting for source…"; update(); return }
            if (operation in listOf("AUTHORIZE", "REQUEST_CONTROL") && data.optBoolean("authorized")) { controlProposal.clear(); session.approve(reply.getString("hostEpoch")); ble.resumeReconnection(); status = "Connected to DX340" }
            if (reply.optString("hostEpoch") != epoch) { disconnect(); return }
            data.optJSONObject("state")?.let(::applyState)
            if (type == "STATE_CHANGED") applyState(data)
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
            if (data.optString("outcome") == "dispatched") { status = "Sent to player; waiting for observed playback."; send("GET_STATE") }
            if (data.optString("outcome") == "observed") status = "Playback state confirmed by source."
            val snapshot = JSONObject().put("state", state).put("sources", sources).put("library", library).toString()
            if (snapshot.toByteArray().size <= 262144) runCatching { snapshotFile.writeText(snapshot) }
            update()
        } catch (_: Exception) { disconnect(); status = "Invalid host reply; reconnect."; update() }
    }
    private fun isAssistedReady(snapshot: JSONObject): Boolean {
        val caps = snapshot.optJSONObject("capabilities") ?: return false
        return caps.optBoolean(if (caps.optString("browseMode") == "ui-assisted") "canBrowse" else "canBrowseFolders")
    }
    val assistedBrowsing get() = library.optBoolean("uiAssisted") || state.optJSONObject("capabilities")?.optString("browseMode") == "ui-assisted"
    val assistedReady get() = authorized && isAssistedReady(state)
    private fun applyState(new: JSONObject) {
        if (state.optString("sourceEpoch") != new.optString("sourceEpoch") || state.optString("source") != new.optString("source") || state.optJSONObject("capabilities")?.optString("browseMode") != new.optJSONObject("capabilities")?.optString("browseMode")) {
            library = JSONObject(); queue = JSONArray(); queueRevision = ""; queuePage = JSONObject(); artworkBytes = null; activeBrowseId = null; activeQueueId = null; browseLocations.clear(); queueLocations.clear()
        } else if (isAssistedReady(new) != isAssistedReady(state) &&
            (assistedBrowsing || activeBrowseId?.let { browseLocations[it]?.parent?.startsWith("mango-ui:") } == true)) {
            // Keep native pages/queues, but never republish an assisted frame across grant/sleep changes.
            library = JSONObject(); activeBrowseId = null; browseLocations.clear()
        }
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
        "LOCAL_APPROVAL_REQUIRED" -> "Approve this controller on DX340, then retry authorization."
        else -> "Action failed ($code). Refresh the player to check its state."
    }
}

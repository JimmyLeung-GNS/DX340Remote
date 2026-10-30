package org.dx340.remote.host

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File

/** Debug APK only; sender must hold signature/privileged DUMP permission (ADB shell). No network. */
class DebugUiProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val main = Handler(Looper.getMainLooper())
        val startedAt = android.os.SystemClock.elapsedRealtime()
        var finished = false
        var pendingReleased = false
        fun releasePending() { if (!pendingReleased) { pendingReleased = true; pending.finish() } }
        val reportTag = intent.getStringExtra("reportTag")
        if (reportTag != null && !reportTag.matches(Regex("[A-Za-z0-9_-]{1,48}"))) { pending.finish(); return }
        val reportFile = File(context.filesDir, if (reportTag == null) "apple-ui-live-test.json" else "debug-probe-$reportTag.json")
        reportFile.writeText(JSONObject().put("time", System.currentTimeMillis()).put("phase", "STARTED")
            .put("operation", intent.getStringExtra("operation") ?: "BROWSE").toString())
        val before = HostService.instance?.sources?.observedSources()
        var interruption: JSONObject? = null
        fun finish(data: JSONObject?, error: String?) {
            if (finished) return
            finished = true
            try {
                val report = JSONObject().put("kind", "physical-own-accessibility-adapter-debug-probe-no-ble")
                    .put("time", System.currentTimeMillis()).put("operation", intent.getStringExtra("operation") ?: "BROWSE")
                    .put("phase", "FINISHED").put("requestedSource", intent.getStringExtra("source") ?: "com.apple.android.music")
                    .put("elapsedMs", android.os.SystemClock.elapsedRealtime() - startedAt)
                    .put("interruption", interruption ?: JSONObject.NULL)
                    .put("data", data ?: JSONObject.NULL).put("error", error ?: JSONObject.NULL)
                    .put("state", HostService.instance?.sources?.state() ?: JSONObject.NULL)
                    .put("sourceStatesBefore", before ?: JSONObject.NULL)
                    .put("sourceStatesAfter", HostService.instance?.sources?.observedSources() ?: JSONObject.NULL)
                reportFile.writeText(report.toString(2))
            } finally { releasePending() }
        }
        val operation = intent.getStringExtra("operation") ?: "BROWSE"
        val asynchronous = operation in setOf("VALIDATE_MANGO_UI_ITEM_FRESH", "PLAY_ITEM")
        val budgetMs = when (operation) {
            "VALIDATE_MANGO_UI_ITEM_FRESH" -> 25000L
            // Up to3.5s source handoff,20s folder refresh and1.2s observation delay.
            "PLAY_ITEM" -> 30000L
            else -> 9500L
        }
        val deadline = startedAt + budgetMs
        fun stillActive() = !finished && android.os.SystemClock.elapsedRealtime() < deadline
        main.postDelayed({ finish(null, "DEBUG_PROBE_TIMEOUT") }, budgetMs)
        try {
            val sources = HostService.instance?.sources ?: return finish(null, "START_COMPANION_FIRST")
            require(!asynchronous || reportTag != null) { "REPORT_TAG_REQUIRED" }
            sources.select(intent.getStringExtra("source") ?: "com.apple.android.music")
            if (intent.hasExtra("changeSourceAfterMs")) {
                require(operation == "BROWSE") { "INVALID_ARGUMENT" }
                val delay = intent.getIntExtra("changeSourceAfterMs", -1)
                val next = intent.getStringExtra("changeSourceTo")
                require(delay in 100..1000 && next in setOf("com.ibasso.music", "com.apple.android.music") && next != sources.selected) { "INVALID_ARGUMENT" }
                main.postDelayed({
                    if (!finished) {
                        interruption = JSONObject().put("kind", "in-process-source-change-while-browse-pending")
                            .put("time", System.currentTimeMillis()).put("from", sources.selected).put("to", next)
                        sources.select(next!!)
                    }
                }, delay.toLong())
            }
            if (operation == "PROBE_VOLUME_DOWN") {
                val state = sources.state()
                require(intent.getStringExtra("sourceEpoch") == state.optString("sourceEpoch")) { "STALE_SOURCE" }
                val audio = context.getSystemService(android.media.AudioManager::class.java)
                val stream = android.media.AudioManager.STREAM_MUSIC
                val current = audio.getStreamVolume(stream)
                require(intent.hasExtra("expectedVolume") && current == intent.getIntExtra("expectedVolume", -1)) { "STALE_VOLUME" }
                require(!audio.isVolumeFixed && audio.isMusicActive && state.optBoolean("playing")) { "VOLUME_PROBE_NOT_READY" }
                require(current > audio.getStreamMinVolume(stream)) { "VOLUME_AT_MINIMUM" }
                val requested = current - 1
                // Absolute, downward-only diagnostic: retrying the same expected value cannot
                // lower the volume twice. No Bluetooth capability is enabled by this probe.
                audio.setStreamVolume(stream, requested, 0)
                main.postDelayed({
                    try {
                        val observed = audio.getStreamVolume(stream)
                        finish(JSONObject().put("kind", "physical-public-volume-api-downward-probe-no-ble")
                            .put("before", current).put("requested", requested).put("observed", observed)
                            .put("note", "Reported API values; listening confirmation required"), if (observed == requested) null else "VOLUME_NOT_OBSERVED")
                    } catch (e: Exception) { finish(null, e.message ?: "VOLUME_PROBE_FAILED") }
                }, 500)
            } else if (operation == "CHECK_CONTROLLER_APPROVALS") {
                Thread({
                    try { val report = ControllerApprovalCheck(context.applicationContext).run(); main.post { finish(report, null) } }
                    catch (e: Exception) { main.post { finish(null, e.message ?: "CHECK_FAILED") } }
                }, "ControllerApprovalCheck").start()
            } else if (operation == "CHECK_MANGO_CAPABILITIES") {
                Thread({
                    try {
                        val report = MangoCapabilityCheck(context.applicationContext).run()
                        main.post { finish(report, if (report.optBoolean("passed")) null else report.optString("error", "CHECK_FAILED")) }
                    } catch (e: Exception) { main.post { finish(null, e.message ?: "CHECK_FAILED") } }
                }, "MangoCapabilityCheck").start()
            } else if (operation == "SET_MANGO_UI_MODE") {
                val enabled = intent.getBooleanExtra("enabled", false)
                context.getSharedPreferences("mango-ui", Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).commit()
                sources.externalStateChanged(); finish(JSONObject().put("enabled", enabled), null)
            } else if (operation == "DESCRIBE_ITEM") {
                finish(sources.nativeItemEvidence(intent.getStringExtra("item") ?: ""), null)
            } else if (operation == "VALIDATE_MANGO_UI_ITEM_FRESH") {
                require(sources.selected == "com.ibasso.music") { "INVALID_SOURCE" }
                val item = intent.getStringExtra("item") ?: error("INVALID_ARGUMENT")
                val revision = intent.getStringExtra("revision") ?: error("INVALID_ARGUMENT")
                require(item.startsWith("mango-ui:") && revision.isNotBlank()) { "INVALID_ARGUMENT" }
                val service = MangoAccessibilityService.instance ?: return finish(null, "UI_PERMISSION_REQUIRED")
                val expectedEpoch = sources.state().optString("sourceEpoch")
                service.validateFreshPlayable(item, revision, { stillActive() && sources.selected == "com.ibasso.music" && sources.state().optString("sourceEpoch") == expectedEpoch }) { issue ->
                    finish(JSONObject().put("validationOnly", true).put("dispatchedPlayback", false), issue)
                }
                // Host foreground service owns this bounded read-only job. Do not keep
                // an Android broadcast open across a potentially20-second UI refresh.
                releasePending()
            } else if (operation == "VALIDATE_MANGO_UI_ITEM") {
                require(sources.selected == "com.ibasso.music") { "INVALID_SOURCE" }
                val item = intent.getStringExtra("item") ?: error("INVALID_ARGUMENT")
                val revision = intent.getStringExtra("revision") ?: error("INVALID_ARGUMENT")
                require(item.startsWith("mango-ui:") && revision.isNotBlank()) { "INVALID_ARGUMENT" }
                val service = MangoAccessibilityService.instance ?: return finish(null, "UI_PERMISSION_REQUIRED")
                // Exact first-stage production validator, without preparePlayback or play.
                val issue = service.validatePlayable(item, revision)
                finish(JSONObject().put("validationOnly", true).put("dispatchedPlayback", false), issue)
            } else if (operation == "GET_STATE") {
                finish(sources.state(), null)
            } else if (operation == "GET_QUEUE") {
                val args = JSONObject()
                if (intent.hasExtra("offset")) args.put("offset", intent.getIntExtra("offset", -1))
                intent.getStringExtra("revision")?.let { args.put("revision", it) }
                finish(sources.queueState(args), null)
            } else if (operation == "CHECK_APPLE_QUEUE_BASELINE") {
                val candidate = intent.getStringExtra("candidate") ?: error("INVALID_ARGUMENT")
                require(sources.selected == "com.apple.android.music") { "INVALID_SOURCE" }
                val manager = context.getSystemService(android.media.session.MediaSessionManager::class.java)
                val controller = manager.getActiveSessions(android.content.ComponentName(context, SessionListener::class.java))
                    .singleOrNull { it.packageName == sources.selected } ?: error("SOURCE_NOT_READY")
                val queue = controller.queue ?: error("UNSUPPORTED_CAPABILITY")
                require(queue.size <= 10000) { "QUEUE_LIMIT" }
                finish(JSONObject().put("kind", "complete-public-Apple-queue-read-no-playback")
                    .put("candidateTitle", candidate).put("count", queue.size)
                    .put("candidateInQueue", queue.any { it.description.title?.toString() == candidate })
                    .put("queue", org.json.JSONArray().apply { queue.forEach {
                        put(JSONObject().put("id", it.queueId.toString()).put("title", it.description.title?.toString()))
                    } }), null)
            } else if (operation == "SET_PLAYING") {
                sources.setPlaying(intent.getBooleanExtra("playing", false), stillAuthorized = ::stillActive) { error ->
                    finish(if (error == null) JSONObject().put("outcome", "observed") else null, error)
                }
            } else if (operation == "PLAY_QUEUE_ITEM") {
                sources.playQueueItem(JSONObject().put("item", intent.getStringExtra("item")).put("revision", intent.getStringExtra("revision")), ::stillActive) { error ->
                    if (error != null) finish(null, error)
                    else main.postDelayed({ finish(JSONObject().put("outcome", "dispatched; inspect observed state separately"), null) }, 1200)
                }
            } else if (operation == "SNAPSHOT_WINDOWS" && sources.selected == "com.ibasso.music") {
                val snapshot = MangoAccessibilityService.instance?.debugWindowSnapshot()
                finish(snapshot, if (snapshot == null) "UI_PERMISSION_REQUIRED" else null)
            } else if (operation == "SNAPSHOT") {
                val serviceSnapshot = if (sources.selected == "com.ibasso.music") MangoAccessibilityService.instance?.debugSnapshot() else AppleMusicAccessibilityService.instance?.debugSnapshot()
                finish(serviceSnapshot, if (serviceSnapshot == null) "UI_PERMISSION_REQUIRED" else null)
            } else if (operation == "PLAY_ITEM") {
                sources.playItem(JSONObject().put("item", intent.getStringExtra("item")).put("revision", intent.getStringExtra("revision")), ::stillActive) { error ->
                    if (error != null) finish(null, error)
                    else main.postDelayed({ finish(JSONObject().put("outcome", "dispatched; inspect observed state separately"), null) }, 1200)
                }
                // The foreground host owns the bounded operation; polling this unique
                // receipt must never rebroadcast a possibly dispatched playback command.
                releasePending()
            } else if (operation == "BROWSE") {
                sources.browse(intent.getStringExtra("parent"), intent.getIntExtra("page", 0), intent.getStringExtra("revision"), ::finish)
            } else finish(null, "UNSUPPORTED_OPERATION")
        } catch (e: Exception) { finish(null, e.message ?: "DEBUG_PROBE_FAILED") }
    }
}

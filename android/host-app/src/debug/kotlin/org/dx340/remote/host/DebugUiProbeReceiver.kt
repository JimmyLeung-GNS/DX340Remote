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
        var finished = false
        val before = HostService.instance?.sources?.observedSources()
        fun finish(data: JSONObject?, error: String?) {
            if (finished) return
            finished = true
            try {
                val report = JSONObject().put("kind", "physical-own-accessibility-adapter-debug-probe-no-ble")
                    .put("time", System.currentTimeMillis()).put("operation", intent.getStringExtra("operation") ?: "BROWSE")
                    .put("data", data ?: JSONObject.NULL).put("error", error ?: JSONObject.NULL)
                    .put("state", HostService.instance?.sources?.state() ?: JSONObject.NULL)
                    .put("sourceStatesBefore", before ?: JSONObject.NULL)
                    .put("sourceStatesAfter", HostService.instance?.sources?.observedSources() ?: JSONObject.NULL)
                File(context.filesDir, "apple-ui-live-test.json").writeText(report.toString(2))
            } finally { pending.finish() }
        }
        main.postDelayed({ finish(null, "DEBUG_PROBE_TIMEOUT") }, 9500)
        try {
            val sources = HostService.instance?.sources ?: return finish(null, "START_COMPANION_FIRST")
            sources.select(intent.getStringExtra("source") ?: "com.apple.android.music")
            val operation = intent.getStringExtra("operation") ?: "BROWSE"
            if (operation == "SET_MANGO_UI_MODE") {
                val enabled = intent.getBooleanExtra("enabled", false)
                context.getSharedPreferences("mango-ui", Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).commit()
                sources.externalStateChanged(); finish(JSONObject().put("enabled", enabled), null)
            } else if (operation == "DESCRIBE_ITEM") {
                finish(sources.nativeItemEvidence(intent.getStringExtra("item") ?: ""), null)
            } else if (operation == "GET_STATE") {
                finish(sources.state(), null)
            } else if (operation == "GET_QUEUE") {
                finish(sources.queueState(), null)
            } else if (operation == "SET_PLAYING") {
                sources.setPlaying(intent.getBooleanExtra("playing", false), stillAuthorized = { true }) { error ->
                    finish(if (error == null) JSONObject().put("outcome", "observed") else null, error)
                }
            } else if (operation == "PLAY_QUEUE_ITEM") {
                sources.playQueueItem(JSONObject().put("item", intent.getStringExtra("item")).put("revision", intent.getStringExtra("revision")), { true }) { error ->
                    if (error != null) finish(null, error)
                    else main.postDelayed({ finish(JSONObject().put("outcome", "dispatched; inspect observed state separately"), null) }, 1200)
                }
            } else if (operation == "SNAPSHOT") {
                val serviceSnapshot = if (sources.selected == "com.ibasso.music") MangoAccessibilityService.instance?.debugSnapshot() else AppleMusicAccessibilityService.instance?.debugSnapshot()
                finish(serviceSnapshot, if (serviceSnapshot == null) "UI_PERMISSION_REQUIRED" else null)
            } else if (operation == "PLAY_ITEM") {
                sources.playItem(JSONObject().put("item", intent.getStringExtra("item")).put("revision", intent.getStringExtra("revision")), { true }) { error ->
                    if (error != null) finish(null, error)
                    else main.postDelayed({ finish(JSONObject().put("outcome", "dispatched; inspect observed state separately"), null) }, 1200)
                }
            } else if (operation == "BROWSE") {
                sources.browse(intent.getStringExtra("parent"), intent.getIntExtra("page", 0), intent.getStringExtra("revision"), ::finish)
            } else finish(null, "UNSUPPORTED_OPERATION")
        } catch (e: Exception) { finish(null, e.message ?: "DEBUG_PROBE_FAILED") }
    }
}

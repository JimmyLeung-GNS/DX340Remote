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
        fun finish(data: JSONObject?, error: String?) {
            if (finished) return
            finished = true
            try {
                val report = JSONObject().put("kind", "physical-own-accessibility-adapter-debug-probe-no-ble")
                    .put("time", System.currentTimeMillis()).put("operation", intent.getStringExtra("operation") ?: "BROWSE")
                    .put("data", data ?: JSONObject.NULL).put("error", error ?: JSONObject.NULL)
                    .put("state", HostService.instance?.sources?.state() ?: JSONObject.NULL)
                File(context.filesDir, "apple-ui-live-test.json").writeText(report.toString(2))
            } finally { pending.finish() }
        }
        main.postDelayed({ finish(null, "DEBUG_PROBE_TIMEOUT") }, 8500)
        try {
            val sources = HostService.instance?.sources ?: return finish(null, "START_COMPANION_FIRST")
            sources.select("com.apple.android.music")
            val operation = intent.getStringExtra("operation") ?: "BROWSE"
            if (operation == "SNAPSHOT") {
                finish(AppleMusicAccessibilityService.instance?.debugSnapshot(), null)
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

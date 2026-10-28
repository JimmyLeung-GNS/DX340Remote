package org.dx340.remote.host

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Physical Activity recreation with a labelled synthetic pending-test fixture; no playback sent. */
class UiRecreationProbe(private val test: Instrumentation) {
    fun run() {
        val context = test.targetContext
        val result = JSONObject().put("kind", "physical-activity-recreation-with-synthetic-pending-fixture").put("audioCommandSent", false)
        val pending = File(context.filesDir, "pending-mango-test.json")
        var activity: Activity? = null
        var ownsFixture = false
        try {
            check(!pending.exists()) { "A real pending test exists; finish it first" }
            test.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("developmentProbe", true))
            val until = android.os.SystemClock.elapsedRealtime() + 12000
            var ready = false
            while (!ready && android.os.SystemClock.elapsedRealtime() < until) { test.runOnMainSync { ready = HostService.instance?.sources?.sources()?.getJSONObject(0)?.optString("browser") == "CONNECTED" }; if (!ready) Thread.sleep(100) }
            var epoch = ""
            test.runOnMainSync { epoch = HostService.instance?.sources?.epoch("com.ibasso.music") ?: error("Host service unavailable") }
            pending.writeText(JSONObject().put("item", JSONObject().put("id", "lifecycle-fixture").put("title", "Synthetic lifecycle fixture - no playback dispatched"))
                .put("revision", epoch).put("page", JSONObject()).put("parents", JSONArray()).toString())
            ownsFixture = true
            activity = test.startActivitySync(Intent(context, BrowseTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            fun hasResult(view: View): Boolean = (view is Button && view.text.toString() == "Correct selected track is audible") || (view is ViewGroup && (0 until view.childCount).any { hasResult(view.getChildAt(it)) })
            var before = false
            test.runOnMainSync { before = hasResult(activity!!.window.decorView) }
            val monitor = test.addMonitor(BrowseTestActivity::class.java.name, null, false)
            test.runOnMainSync { activity!!.recreate() }
            activity = test.waitForMonitorWithTimeout(monitor, 6000) ?: error("Recreated Activity not observed")
            test.removeMonitor(monitor)
            var after = false
            test.runOnMainSync { after = hasResult(activity!!.window.decorView) }
            result.put("resultControlsBefore", before).put("resultControlsAfter", after).put("pendingFilePreserved", pending.isFile)
                .put("passed", before && after && pending.isFile)
        } catch (e: Exception) { result.put("passed", false).put("error", e.message) }
        finally {
            if (ownsFixture) pending.delete()
            test.runOnMainSync { activity?.finish() }
        }
        File(context.filesDir, "ui-recreation-probe.json").writeText(result.toString(2))
        test.finish(Activity.RESULT_OK, Bundle().apply { putString("report", result.toString()) })
    }
}

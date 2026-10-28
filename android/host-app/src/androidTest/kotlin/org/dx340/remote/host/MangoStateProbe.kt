package org.dx340.remote.host

import android.app.Instrumentation
import android.content.ComponentName
import android.os.Bundle
import org.dx340.remote.sources.SourceRegistry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Tests the actual adapter's guarded SET_PLAYING semantics against physical Mango. */
class MangoStateProbe(private val test: Instrumentation) {
    fun run() {
        val context = test.targetContext
        val report = JSONObject().put("kind", "physical-mango-observed-state-adapter").put("time", System.currentTimeMillis())
            .put("device", android.os.Build.MODEL).put("build", android.os.Build.DISPLAY).put("audio", "Session observations only; no independent audible judgment")
        val steps = JSONArray()
        var registry: SourceRegistry? = null
        var restorePlaying = false
        fun state(): JSONObject { var value = JSONObject(); test.runOnMainSync { value = registry!!.state() }; return value }
        fun set(playing: Boolean): String? {
            val done = CountDownLatch(1); var error: String? = "SOURCE_TIMEOUT"
            test.runOnMainSync { registry!!.setPlaying(playing, experimental = true, stillAuthorized = { true }) { error = it; done.countDown() } }
            done.await(5, TimeUnit.SECONDS); return error
        }
        try {
            val ready = CountDownLatch(1)
            test.runOnMainSync {
                registry = SourceRegistry(context, ComponentName(context, SessionListener::class.java)) {
                    if (registry?.sources()?.getJSONObject(0)?.optString("browser") == "CONNECTED") ready.countDown()
                }
                registry!!.start(); registry!!.select("com.ibasso.music")
            }
            check(ready.await(12, TimeUnit.SECONDS)) { "Mango browser not ready" }
            val original = state(); restorePlaying = original.optBoolean("playing")
            check(restorePlaying) { "Start a Mango track before the experiment" }
            val title = original.getString("title")
            var passed = true
            for (desired in listOf(false, false, true, true)) {
                val start = android.os.SystemClock.elapsedRealtime()
                val error = set(desired)
                Thread.sleep(500)
                val actual = state()
                val success = error == null && actual.optBoolean("playing") == desired && actual.optString("title") == title
                passed = passed && success
                steps.put(JSONObject().put("desiredPlaying", desired).put("observedPlaying", actual.optBoolean("playing"))
                    .put("title", actual.optString("title")).put("error", error ?: JSONObject.NULL).put("passed", success)
                    .put("elapsedMsIncluding500msStabilityWait", android.os.SystemClock.elapsedRealtime() - start))
                if (!success) break
            }
            val pi = context.packageManager.getPackageInfo("com.ibasso.music", 0)
            report.put("version", pi.versionName).put("versionCode", pi.longVersionCode).put("passed", passed).put("steps", steps)
            val file = File(context.filesDir, "mango-observed-state-probe.json")
            java.io.FileOutputStream(file).use { it.write(report.toString(2).toByteArray()); it.fd.sync() }
            val prefs = context.getSharedPreferences("source-verification", android.content.Context.MODE_PRIVATE)
            val edit = prefs.edit()
            if (passed) edit.putString("mangoObservedPlayVersion", "${pi.versionName}|${pi.longVersionCode}|${android.os.Build.FINGERPRINT}") else edit.remove("mangoObservedPlayVersion")
            check(edit.commit()) { "Cannot persist observed capability" }
        } catch (e: Exception) { report.put("passed", false).put("error", e.message).put("steps", steps) }
        finally {
            if (registry != null && restorePlaying) report.put("restoreError", set(true) ?: JSONObject.NULL)
            test.runOnMainSync { registry?.close() }
        }
        File(context.filesDir, "mango-observed-state-probe.json").writeText(report.toString(2))
        test.finish(android.app.Activity.RESULT_OK, Bundle().apply { putString("report", report.toString()) })
    }
}

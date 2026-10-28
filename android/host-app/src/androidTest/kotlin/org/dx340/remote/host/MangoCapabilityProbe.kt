package org.dx340.remote.host

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.ContextWrapper
import org.dx340.remote.sources.SourceRegistry
import org.dx340.remote.sources.UiLibraryAdapter
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real native Mango session plus explicitly simulated UI readiness; never dispatches playback. */
class MangoCapabilityProbe(private val instrumentation: Instrumentation) {
    fun run() {
        val context = instrumentation.targetContext
        val temporaryPreferences = "capability-probe-${UUID.randomUUID()}"
        val isolated = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences(
                if (name == "source-verification") temporaryPreferences else name, mode)
        }
        var uiCalls = 0
        val ui = object : UiLibraryAdapter {
            override val enabled = true
            override val ready = true
            override val explanation = "SIMULATED UI readiness for native capability isolation only"
            override fun browse(parent: String?, page: Int, revision: String?, stillAuthorized: () -> Boolean, done: (JSONObject?, String?) -> Unit) { uiCalls++; done(null, "UNEXPECTED_UI_CALL") }
            override fun validatePlayable(item: String, revision: String): String { uiCalls++; return "UNEXPECTED_UI_CALL" }
            override fun play(item: String, revision: String, stillAuthorized: () -> Boolean, done: (String?) -> Unit) { uiCalls++; done("UNEXPECTED_UI_CALL") }
        }
        val report = JSONObject().put("kind", "component-test-real-native-session-simulated-ui-readiness-no-playback")
            .put("device", android.os.Build.MODEL).put("build", android.os.Build.DISPLAY)
        var registry: SourceRegistry? = null
        fun onMain(action: () -> Unit) {
            var failure: Exception? = null
            instrumentation.runOnMainSync { try { action() } catch (e: Exception) { failure = e } }
            failure?.let { throw it }
        }
        fun browse(parent: String?): JSONObject {
            val latch = CountDownLatch(1); var result: JSONObject? = null; var failure: String? = null
            onMain { registry!!.browse(parent, 0) { data, error -> result = data; failure = error; latch.countDown() } }
            check(latch.await(10, TimeUnit.SECONDS)) { "BROWSE_TIMEOUT" }; check(failure == null) { failure ?: "BROWSE_FAILED" }
            return result ?: error("NO_BROWSE_RESULT")
        }
        try {
            val ready = CountDownLatch(1)
            onMain {
                registry = SourceRegistry(isolated, ComponentName(context, SessionListener::class.java), mangoUiLibrary = ui) {
                    val sources = registry?.sources()
                    if (sources != null && (0 until sources.length()).any { sources.getJSONObject(it).optString("id") == "com.ibasso.music" && sources.getJSONObject(it).optString("browser") == "CONNECTED" }) ready.countDown()
                }
                registry!!.start(); registry!!.select("com.ibasso.music")
            }
            check(ready.await(12, TimeUnit.SECONDS)) { "MANGO_BROWSER_NOT_READY" }
            onMain {
                val reg = registry!!; val state = reg.state(); val caps = state.getJSONObject("capabilities")
                report.put("stateBefore", state).put("capabilities", caps)
            }
            val caps = report.getJSONObject("capabilities")
            check(caps.optBoolean("canPlayItem") && !caps.optBoolean("canPlayNativeItem")) { "TEST_PRECONDITION_NATIVE_CAPABILITY" }
            var nativeError: String? = null
            onMain {
                val reg = registry!!
                // An unknown native ID makes this safe even if the capability guard regresses.
                reg.playItem(JSONObject().put("item", "native-capability-probe-unknown-id").put("revision", reg.epoch(reg.selected)), { true }) { error ->
                    nativeError = error
                }
            }
            report.put("nativeRequestError", nativeError)
            check(nativeError == "UNSUPPORTED_CAPABILITY") { "NATIVE_CAPABILITY_PROMOTED_OR_NO_REPLY:$nativeError" }
            val root = browse(null); val roots = root.getJSONArray("items")
            val allMusic = (0 until roots.length()).map(roots::getJSONObject).first { it.optString("title") == "All Music" }
            val items = browse(allMusic.getString("id")).getJSONArray("items")
            check(items.length() > 0) { "NO_NATIVE_ITEMS" }
            check((0 until items.length()).all { !items.getJSONObject(it).optBoolean("playable") }) { "UNVERIFIED_NATIVE_ROW_PLAYABLE" }
            check((0 until items.length()).any { items.getJSONObject(it).optBoolean("nativeTestable") }) { "NO_EXPLICIT_TEST_ITEMS" }
            check(uiCalls == 0) { "NATIVE_REQUEST_REACHED_UI" }
            onMain { report.put("stateAfter", registry!!.state()) }
            report.put("nativeRowsChecked", items.length()).put("uiCalls", uiCalls).put("passed", true)
        } catch (e: Exception) { report.put("passed", false).put("error", e.message) }
        finally { onMain { registry?.close() }; context.deleteSharedPreferences(temporaryPreferences) }
        File(context.filesDir, "mango-capability-probe.json").writeText(report.toString(2))
        instrumentation.finish(if (report.optBoolean("passed")) Activity.RESULT_OK else Activity.RESULT_CANCELED, android.os.Bundle().apply { putString("report", report.toString()) })
    }
}

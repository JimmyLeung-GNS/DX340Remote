package org.dx340.remote.host

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.dx340.remote.sources.SourceRegistry
import org.dx340.remote.sources.UiLibraryAdapter
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real native Mango session plus explicitly simulated UI readiness; never dispatches playback. */
class MangoCapabilityCheck(private val context: Context) {
    /** Run off the main thread. The debug receiver can use this without restarting the companion. */
    fun run(): JSONObject {
        check(Looper.myLooper() != Looper.getMainLooper()) { "CHECK_REQUIRES_WORKER_THREAD" }
        val main = Handler(Looper.getMainLooper())
        val deadline = SystemClock.elapsedRealtime() + 8000
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
            val latch = CountDownLatch(1)
            main.post { try { if (SystemClock.elapsedRealtime() <= deadline) action() else error("CHECK_TIMEOUT") } catch (e: Exception) { failure = e } finally { latch.countDown() } }
            check(latch.await(maxOf(1, deadline - SystemClock.elapsedRealtime()), TimeUnit.MILLISECONDS)) { "CHECK_TIMEOUT" }
            failure?.let { throw it }
        }
        fun browse(parent: String?): JSONObject {
            val latch = CountDownLatch(1); var result: JSONObject? = null; var failure: String? = null
            onMain { registry!!.browse(parent, 0) { data, error -> result = data; failure = error; latch.countDown() } }
            check(latch.await(maxOf(1, deadline - SystemClock.elapsedRealtime()), TimeUnit.MILLISECONDS)) { "BROWSE_TIMEOUT" }; check(failure == null) { failure ?: "BROWSE_FAILED" }
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
            check(ready.await(maxOf(1, deadline - SystemClock.elapsedRealtime()), TimeUnit.MILLISECONDS)) { "MANGO_BROWSER_NOT_READY" }
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
            // These transitions are simulated ONLY in the isolated temporary preferences.
            // They do not represent listening evidence or change production verification.
            val initialEpoch = root.getString("revision")
            var enabledEpoch = ""; var disabledEpoch = ""; var staleError: String? = null
            onMain {
                val reg = registry!!
                reg.confirmMangoPlayItem(true); enabledEpoch = reg.epoch(reg.selected)
                reg.playItem(JSONObject().put("item", "native-capability-probe-unknown-id").put("revision", initialEpoch), { true }) { staleError = it }
            }
            check(enabledEpoch != initialEpoch && staleError == "STALE_LIBRARY") { "VERIFICATION_DID_NOT_INVALIDATE_OLD_REVISION" }
            val enabledItems = browse(allMusic.getString("id")).getJSONArray("items")
            check((0 until enabledItems.length()).any { enabledItems.getJSONObject(it).optBoolean("playable") }) { "VERIFIED_NATIVE_ROWS_NOT_REFRESHABLE" }
            onMain {
                val reg = registry!!
                reg.confirmMangoPlayItem(false); disabledEpoch = reg.epoch(reg.selected)
            }
            check(disabledEpoch != enabledEpoch) { "REVOCATION_DID_NOT_INVALIDATE_REVISION" }
            val revokedItems = browse(allMusic.getString("id")).getJSONArray("items")
            check((0 until revokedItems.length()).all { !revokedItems.getJSONObject(it).optBoolean("playable") }) { "REVOKED_ROWS_STILL_PLAYABLE" }
            var repeatedEpoch = ""
            onMain { registry!!.confirmMangoPlayItem(false); repeatedEpoch = registry!!.epoch("com.ibasso.music") }
            check(repeatedEpoch == disabledEpoch) { "UNCHANGED_CAPABILITY_RESET_LIBRARY" }
            report.put("simulatedVerificationTransitions", true).put("oldRevisionRejected", true).put("revocationRowsChecked", revokedItems.length())
            check(uiCalls == 0) { "NATIVE_REQUEST_REACHED_UI" }
            onMain { report.put("stateAfter", registry!!.state()) }
            report.put("nativeRowsChecked", items.length()).put("uiCalls", uiCalls).put("passed", true)
        } catch (e: Exception) { report.put("passed", false).put("error", e.message) }
        finally { main.post { registry?.close(); context.deleteSharedPreferences(temporaryPreferences) } }
        File(context.filesDir, "mango-capability-probe.json").writeText(report.toString(2))
        return report
    }
}

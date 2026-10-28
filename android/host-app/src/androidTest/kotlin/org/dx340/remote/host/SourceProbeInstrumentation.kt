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

/** Physical, read-only probes using the installed companion identity; never a simulated source. */
class SourceProbeInstrumentation : Instrumentation() {
    private var mode: String? = null
    private var expectedTitle: String? = null
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); mode = arguments?.getString("mode"); expectedTitle = arguments?.getString("expectedTitle"); start() }
    override fun onStart() {
        if (mode == "apple-queue-baseline") {
            val report = JSONObject().put("kind", "physical-public-apple-queue-before-ui-selection").put("candidateTitle", expectedTitle)
            try {
                val controller = targetContext.getSystemService(android.media.session.MediaSessionManager::class.java)
                    .getActiveSessions(ComponentName(targetContext, SessionListener::class.java)).firstOrNull { it.packageName == "com.apple.android.music" } ?: error("No Apple Music session")
                val queue = controller.queue ?: error("Queue unavailable")
                require(queue.size <= 10000) { "Queue limit" }
                report.put("count", queue.size).put("candidateInQueue", queue.any { it.description.title?.toString() == expectedTitle })
                    .put("currentTitle", controller.metadata?.getString("android.media.metadata.TITLE"))
                    .put("queue", JSONArray().apply { queue.forEach { put(JSONObject().put("id", it.queueId.toString()).put("title", it.description.title?.toString())) } })
            } catch (e: Exception) { report.put("error", e.message) }
            File(targetContext.filesDir, "apple-queue-before-ui.json").writeText(report.toString(2))
            finish(android.app.Activity.RESULT_OK, Bundle().apply { putString("report", report.toString()) }); return
        }
        if (mode == "mango-transports") { MangoTransportProbe(this).run(); return }
        if (mode == "mango-observed-state") { MangoStateProbe(this).run(); return }
        if (mode == "ui-recreation") { UiRecreationProbe(this).run(); return }
        if (mode == "mango-capability-boundary") {
            val report = MangoCapabilityCheck(targetContext).run()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                finish(if (report.optBoolean("passed")) android.app.Activity.RESULT_OK else android.app.Activity.RESULT_CANCELED, Bundle().apply { putString("report", report.toString()) })
            }
            return
        }
        val result = JSONObject().put("kind", "physical-read-only-source-probe")
        var registry: SourceRegistry? = null
        try {
            val ready = CountDownLatch(1)
            runOnMainSync {
                registry = SourceRegistry(targetContext, ComponentName(targetContext, SessionListener::class.java)) {
                    val snapshot = registry?.sources()
                    if (snapshot != null && (0 until snapshot.length()).all { snapshot.getJSONObject(it).optString("browser") !in listOf("CONNECTING", "NOT_PROBED") }) ready.countDown()
                }
                registry!!.start()
            }
            ready.await(12, TimeUnit.SECONDS)
            val reg = registry!!
            val pages = JSONArray()
            for (source in SourceRegistry.PACKAGES) {
                runOnMainSync { reg.select(source) }
                val root = browse(reg, null, 0); pages.put(JSONObject().put("source", source).put("page", root))
                val roots = root.optJSONArray("items") ?: continue
                for (i in 0 until minOf(roots.length(), 8)) {
                    val item = roots.getJSONObject(i)
                    if (!item.optBoolean("browsable")) continue
                    val child = browse(reg, item.getString("id"), 0)
                    pages.put(JSONObject().put("source", source).put("parentTitle", item.optString("title")).put("page", child))
                    val children = child.optJSONArray("items") ?: continue
                    for (j in 0 until minOf(children.length(), 2)) {
                        val album = children.getJSONObject(j)
                        if (album.optBoolean("browsable")) pages.put(JSONObject().put("source", source).put("parentTitle", album.optString("title")).put("page", browse(reg, album.getString("id"), 0)))
                    }
                }
            }
            runOnMainSync { result.put("diagnostic", reg.diagnostic()) }
            result.put("pages", pages)
            result.put("media3Apple", probeMedia3())
        } catch (e: Exception) { result.put("error", e.toString()) }
        finally { runOnMainSync { registry?.close() } }
        File(targetContext.filesDir, "source-probe.json").writeText(result.toString(2))
        finish(android.app.Activity.RESULT_OK, Bundle().apply { putString("result", "files/source-probe.json; no playback command sent") })
    }
    private fun browse(reg: SourceRegistry, parent: String?, page: Int): JSONObject {
        val ready = CountDownLatch(1); var value = JSONObject()
        runOnMainSync { reg.browse(parent, page) { data, error -> value = data ?: JSONObject().put("error", error); ready.countDown() } }
        if (!ready.await(10, TimeUnit.SECONDS)) return JSONObject().put("error", "TIMEOUT")
        return value
    }
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun probeMedia3(): JSONObject {
        val report = JSONObject().put("client", targetContext.packageName)
        var browser: androidx.media3.session.MediaBrowser? = null
        return try {
            val token = androidx.media3.session.SessionToken(targetContext, ComponentName("com.apple.android.music", "com.apple.android.music.player.MediaPlaybackService"))
            var future: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaBrowser>? = null
            runOnMainSync { future = androidx.media3.session.MediaBrowser.Builder(targetContext, token).setApplicationLooper(android.os.Looper.getMainLooper()).buildAsync() }
            browser = future!!.get(12, TimeUnit.SECONDS)
            var rootFuture: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.LibraryResult<androidx.media3.common.MediaItem>>? = null
            val current = browser
            runOnMainSync { rootFuture = current.getLibraryRoot(null) }
            val root = rootFuture!!.get(8, TimeUnit.SECONDS)
            report.put("connected", true).put("rootResultCode", root.resultCode).put("rootId", root.value?.mediaId)
        } catch (e: Exception) { report.put("connected", false).put("error", e.javaClass.simpleName).put("detail", e.cause?.javaClass?.simpleName ?: e.message?.take(250)) }
        finally { val current = browser; if (current != null) runOnMainSync { current.release() } }
    }
}

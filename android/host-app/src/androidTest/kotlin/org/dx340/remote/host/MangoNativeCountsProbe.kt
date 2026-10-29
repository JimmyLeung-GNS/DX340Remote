package org.dx340.remote.host

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.media.browse.MediaBrowser
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Raw public MediaBrowser counts; bypasses SourceRegistry pagination/cache, never controls playback. */
class MangoNativeCountsProbe(private val test: Instrumentation) {
    fun run() {
        val context = test.targetContext
        val report = JSONObject().put("kind", "physical-mango-raw-public-browser-counts-no-playback")
            .put("time", System.currentTimeMillis()).put("device", android.os.Build.MODEL)
            .put("build", android.os.Build.DISPLAY).put("client", context.packageName)
        var browser: MediaBrowser? = null
        val samples = JSONArray()
        try {
            val services = context.packageManager.queryIntentServices(
                Intent("android.media.browse.MediaBrowserService").setPackage("com.ibasso.music"), 0)
            require(services.size == 1) { "EXPECTED_ONE_MANGO_BROWSER" }
            val info = services.single().serviceInfo
            val connected = CountDownLatch(1)
            var connectionError: String? = null
            test.runOnMainSync {
                browser = MediaBrowser(context, ComponentName(info.packageName, info.name), object : MediaBrowser.ConnectionCallback() {
                    override fun onConnected() { connected.countDown() }
                    override fun onConnectionFailed() { connectionError = "BROWSE_DENIED"; connected.countDown() }
                }, null)
                browser!!.connect()
            }
            check(connected.await(12, TimeUnit.SECONDS)) { "CONNECT_TIMEOUT" }
            check(connectionError == null) { connectionError!! }
            val active = browser!!
            fun read(parent: String, page: Int? = null, size: Int = 25): List<MediaBrowser.MediaItem> {
                val latch = CountDownLatch(1)
                var value: List<MediaBrowser.MediaItem>? = null
                var error: String? = null
                val callback = object : MediaBrowser.SubscriptionCallback() {
                    private fun loaded(children: List<MediaBrowser.MediaItem>) {
                        if (latch.count == 0L) return
                        if (children.size > 10000) error = "SOURCE_LIBRARY_LIMIT" else value = children.toList()
                        latch.countDown()
                    }
                    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) = loaded(children)
                    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>, options: Bundle) = loaded(children)
                    override fun onError(parentId: String) { error = "BROWSE_DENIED"; latch.countDown() }
                    override fun onError(parentId: String, options: Bundle) = onError(parentId)
                }
                try {
                    test.runOnMainSync {
                        if (page == null) active.subscribe(parent, callback)
                        else active.subscribe(parent, Bundle().apply {
                            putInt(MediaBrowser.EXTRA_PAGE, page); putInt(MediaBrowser.EXTRA_PAGE_SIZE, size)
                        }, callback)
                    }
                    check(latch.await(10, TimeUnit.SECONDS)) { "CHILDREN_TIMEOUT" }
                    check(error == null) { error!! }
                    val rows = value ?: error("MISSING_CHILDREN")
                    samples.put(JSONObject().put("parent", parent).put("requestedPage", page ?: JSONObject.NULL)
                        .put("requestedPageSize", if (page == null) JSONObject.NULL else size)
                        .put("rawCallbackCount", rows.size).put("firstId", rows.firstOrNull()?.mediaId ?: JSONObject.NULL)
                        .put("lastId", rows.lastOrNull()?.mediaId ?: JSONObject.NULL))
                    return rows
                } finally { test.runOnMainSync { if (active.isConnected) active.unsubscribe(parent, callback) } }
            }
            var rootId = ""
            test.runOnMainSync { rootId = active.root }
            val root = read(rootId)
            val allMusic = root.single { it.mediaId == "__ALL_MUSIC__" && it.isBrowsable }.mediaId!!
            val albumsId = root.single { it.mediaId == "__ALL_ALBUM__" && it.isBrowsable }.mediaId!!
            val all = read(allMusic)
            read(allMusic, 0)
            read(allMusic, 19)
            read(allMusic, 20)
            read(allMusic, 0, 1000)
            val albums = read(albumsId)
            val largest = albums.maxBy { it.description.subtitle?.toString()?.toIntOrNull() ?: -1 }
            val tracks = read(largest.mediaId!!)
            report.put("allMusicUnpagedCount", all.size).put("albumCount", albums.size)
                .put("albumReportedTrackSum", albums.sumOf { it.description.subtitle?.toString()?.toIntOrNull() ?: error("INVALID_ALBUM_COUNT") })
                .put("largestAlbumReportedTracks", largest.description.subtitle?.toString()?.toInt())
                .put("largestAlbumRawTracks", tracks.size).put("completed", true)
            val version = context.packageManager.getPackageInfo("com.ibasso.music", 0)
            report.put("sourceVersion", version.versionName).put("sourceVersionCode", version.longVersionCode)
        } catch (e: Exception) { report.put("completed", false).put("error", e.message ?: e.javaClass.simpleName) }
        finally { test.runOnMainSync { browser?.disconnect() } }
        report.put("samples", samples).put("scope", "Source callback counts only; no SourceRegistry cache/pagination, playback or audio judgment")
        File(context.filesDir, "mango-native-counts.json").writeText(report.toString(2))
        test.finish(if (report.optBoolean("completed")) Activity.RESULT_OK else Activity.RESULT_CANCELED,
            Bundle().apply { putString("result", "files/mango-native-counts.json") })
    }
}

package org.dx340.remote.host

import android.app.Instrumentation
import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.SystemClock
import org.json.JSONObject
import java.io.File

/** Actual public-API source experiment, no BLE/audio-measurement claim. */
class MangoTransportProbe(private val test: Instrumentation) {
    fun run() {
        val context = test.targetContext
        val result = JSONObject().put("kind", "physical-mango-explicit-transport-api").put("time", System.currentTimeMillis())
            .put("device", android.os.Build.MODEL).put("build", android.os.Build.DISPLAY)
        var controller: MediaController? = null
        var initiallyPlaying = false
        try {
            val manager = context.getSystemService(MediaSessionManager::class.java)
            controller = manager.getActiveSessions(ComponentName(context, SessionListener::class.java)).firstOrNull { it.packageName == "com.ibasso.music" } ?: error("No active Mango session")
            val c = controller
            initiallyPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING
            check(initiallyPlaying) { "Start a Mango track before this experiment" }
            val title = c.metadata?.getString("android.media.metadata.TITLE") ?: error("No track metadata")
            val pi = context.packageManager.getPackageInfo("com.ibasso.music", 0)
            result.put("version", pi.versionName).put("versionCode", pi.longVersionCode).put("track", title).put("advertisedActions", c.playbackState?.actions)
            fun waitFor(state: Int): Boolean {
                val until = SystemClock.elapsedRealtime() + 3500
                while (SystemClock.elapsedRealtime() < until) {
                    if (c.playbackState?.state == state) return c.metadata?.getString("android.media.metadata.TITLE") == title
                    Thread.sleep(100)
                }
                return false
            }
            test.runOnMainSync { c.transportControls.pause() }
            val pause = waitFor(PlaybackState.STATE_PAUSED)
            var pauseIdempotent = false
            if (pause) { test.runOnMainSync { c.transportControls.pause() }; Thread.sleep(700); pauseIdempotent = c.playbackState?.state == PlaybackState.STATE_PAUSED && c.metadata?.getString("android.media.metadata.TITLE") == title }
            test.runOnMainSync { c.transportControls.play() }
            val play = waitFor(PlaybackState.STATE_PLAYING)
            var playIdempotent = false
            if (play) { test.runOnMainSync { c.transportControls.play() }; Thread.sleep(700); playIdempotent = c.playbackState?.state == PlaybackState.STATE_PLAYING && c.metadata?.getString("android.media.metadata.TITLE") == title }
            result.put("explicitPause", pause).put("repeatedPauseStaysPaused", pauseIdempotent).put("explicitPlay", play).put("repeatedPlayStaysPlaying", playIdempotent)
                .put("audio", "No independent audible judgment; session states only, volume untouched")
            val file = File(context.filesDir, "mango-transport-probe.json")
            java.io.FileOutputStream(file).use { it.write(result.toString(2).toByteArray()); it.fd.sync() }
            val version = "${pi.versionName}|${pi.longVersionCode}|${android.os.Build.FINGERPRINT}"
            val prefs = context.getSharedPreferences("source-verification", android.content.Context.MODE_PRIVATE)
            val edit = prefs.edit()
            if (pause && pauseIdempotent) edit.putString("mangoPauseVersion", version) else edit.remove("mangoPauseVersion")
            if (play && playIdempotent) edit.putString("mangoPlayVersion", version) else edit.remove("mangoPlayVersion")
            check(edit.commit()) { "Cannot persist observed capability" }
        } catch (e: Exception) { result.put("error", e.message); File(context.filesDir, "mango-transport-probe.json").writeText(result.toString(2)) }
        finally {
            if (initiallyPlaying && controller?.playbackState?.state == PlaybackState.STATE_PAUSED) test.runOnMainSync { controller?.transportControls?.play() }
        }
        test.finish(android.app.Activity.RESULT_OK, Bundle().apply { putString("report", result.toString()) })
    }
}

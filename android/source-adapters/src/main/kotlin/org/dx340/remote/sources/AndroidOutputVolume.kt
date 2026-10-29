package org.dx340.remote.sources

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import org.json.JSONObject

/** Public STREAM_MUSIC API only; output labels are explicitly confirmed by a listener. */
class AndroidOutputVolume(context: Context, handler: Handler, sourceKey: () -> String,
    sourceReady: () -> Boolean, changed: () -> Unit) {
    private val audio = context.getSystemService(AudioManager::class.java)
    val control = OutputVolume({
        val inventory = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { "${it.id}:${it.type}:${it.address}" }.sorted().joinToString("|")
        VolumeSample(sourceKey() + "|" + Build.FINGERPRINT + "|" + inventory,
            audio.getStreamVolume(AudioManager.STREAM_MUSIC), audio.getStreamMinVolume(AudioManager.STREAM_MUSIC),
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), !audio.isVolumeFixed && audio.isMusicActive && sourceReady())
    }, { value -> audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0) },
        { delay, task -> handler.postDelayed(task, delay) }, changed)
    private val devices = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = control.reset()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = control.reset()
    }
    init { audio.registerAudioDeviceCallback(devices, handler) }
    fun state(): JSONObject {
        return runCatching {
            val sample = control.sample(); val proof = control.proof(sample)
            JSONObject().put("current", sample.current).put("minimum", sample.minimum).put("maximum", sample.maximum)
                .put("verified", proof != null).put("profileId", proof?.id ?: JSONObject.NULL)
                .put("verifiedMinimum", proof?.low ?: JSONObject.NULL).put("verifiedMaximum", proof?.high ?: JSONObject.NULL)
                .put("outputLabel", proof?.label ?: JSONObject.NULL)
                .put("reason", if (proof != null) "Only this locally confirmed range is enabled for this source and output session."
                    else if (!sample.ready) "Play this source on DX340 and verify its output in the companion."
                    else "Verify this source and output in the DX340 companion. Verification resets when the companion stops or an output device changes.")
        }.getOrElse { JSONObject().put("verified", false).put("reason", "Output volume is unavailable. Open the DX340 companion.") }
    }
    fun close() { audio.unregisterAudioDeviceCallback(devices); control.close() }
}

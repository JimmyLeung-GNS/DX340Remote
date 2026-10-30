package org.dx340.remote.sources

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import org.json.JSONObject

/** Main-executor callbacks; no storage permission, scanning, mount or unmount action. */
class StorageVolumeObserver(private val context: Context, private val changed: () -> Unit) {
    private val manager = context.getSystemService(StorageManager::class.java)
    private var changes: StorageVolumeChanges? = null
    private var registered = false
    private var closed = false
    private var events = 0L
    var active = false; private set
    private var status = "NOT_STARTED"
    private fun sample(volume: StorageVolume) = StorageVolumeSample(volume.isPrimary, volume.uuid, volume.state)
    private fun snapshot() = manager.storageVolumes.map(::sample)
    private val callback = object : StorageManager.StorageVolumeCallback() {
        override fun onStateChanged(volume: StorageVolume) {
            if (closed || !registered) return
            if (events < Long.MAX_VALUE) events++
            // Do not re-query the latest snapshot here: it can hide a queued removal.
            if (changes?.observe(sample(volume)) == true) changed()
        }
    }
    fun start() {
        if (closed || registered) return
        try {
            changes = StorageVolumeChanges(snapshot())
            manager.registerStorageVolumeCallback(context.mainExecutor, callback)
            registered = true; active = true; status = "ACTIVE"
            // Cover the snapshot/register gap without coalescing later callbacks.
            if (changes!!.reconcile(snapshot())) changed()
        } catch (_: RuntimeException) {
            active = false; status = "UNAVAILABLE"
            if (registered) runCatching { manager.unregisterStorageVolumeCallback(callback) }
            registered = false
            changed()
        }
    }
    fun diagnostic() = JSONObject().put("status", status).put("active", active)
        .put("callbackEvents", events).put("retainedVolumes", changes?.retainedCount ?: 0)
    fun close() {
        closed = true; active = false; status = "CLOSED"
        changes?.close()
        if (registered) runCatching { manager.unregisterStorageVolumeCallback(callback) }
        registered = false
    }
}

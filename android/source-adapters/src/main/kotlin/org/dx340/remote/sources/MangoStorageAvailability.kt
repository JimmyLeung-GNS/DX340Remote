package org.dx340.remote.sources

/** Public volume state only: a readable removable volume permits an attempt,
 * not a claim that this particular volume is Mango's SD card. UI checks remain required. */
data class RemovableStorageState(val removable: Boolean, val state: String)

object MangoStorageAvailability {
    fun canAttemptSd(volumes: List<RemovableStorageState>?): Boolean =
        volumes != null && volumes.size <= StorageVolumeChanges.MAX_VOLUMES &&
            volumes.any { it.removable && it.state in setOf("mounted", "mounted_ro") }

    const val UNAVAILABLE_HINT = "SD storage is unavailable. Check the card on your player, then browse again."
}

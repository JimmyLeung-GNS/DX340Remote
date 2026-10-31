package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class MangoStorageAvailabilityTest {
    @Test fun mountedInternalStorageDoesNotEnableAnUnavailableSdChoice() {
        assertFalse(MangoStorageAvailability.canAttemptSd(listOf(RemovableStorageState(false, "mounted"))))
        assertFalse(MangoStorageAvailability.canAttemptSd(emptyList()))
    }
    @Test fun bothReadableRemovableMountModesPermitTheExistingGuardedUiAttempt() {
        for (state in listOf("mounted", "mounted_ro")) {
            assertTrue(MangoStorageAvailability.canAttemptSd(listOf(RemovableStorageState(true, state))))
        }
    }
    @Test fun ejectedUnmountedAndTransitionalStatesDoNotPermitOldSdActions() {
        for (state in listOf("unmounted", "removed", "bad_removal", "checking", "ejecting", "shared", "unmountable", "nofs", "unknown", "future-state")) {
            assertFalse(state, MangoStorageAvailability.canAttemptSd(listOf(RemovableStorageState(false, "mounted"), RemovableStorageState(true, state))))
        }
    }
    @Test fun unreadableOrFailedSnapshotCannotGrantAvailability() {
        assertFalse(MangoStorageAvailability.canAttemptSd(null))
        assertFalse(MangoStorageAvailability.canAttemptSd(List(StorageVolumeChanges.MAX_VOLUMES + 1) { RemovableStorageState(true, "mounted") }))
    }
    @Test fun oneReadableRemovableVolumeOnlyPermitsAttemptingTheSourceUi() {
        val volumes = listOf(RemovableStorageState(false, "mounted"), RemovableStorageState(true, "unmounted"), RemovableStorageState(true, "mounted"))
        assertTrue(MangoStorageAvailability.canAttemptSd(volumes))
        assertFalse(MangoStorageAvailability.canAttemptSd(volumes.map { if (it.removable) it.copy(state = "unmounted") else it }))
    }
}

package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class StorageVolumeChangesTest {
    private fun card(id: String? = "card-A", state: String = "mounted") = StorageVolumeSample(false, id, state)
    @Test fun sameUuidUnmountRemountInvalidatesBothEvenWhenLatestStateMatchesBaseline() {
        val changes = StorageVolumeChanges(listOf(card()))
        assertFalse(changes.observe(card()))
        assertTrue(changes.observe(card(state = "unmounted")))
        assertTrue(changes.observe(card()))
        assertFalse(changes.observe(card()))
    }
    @Test fun startupSnapshotGapAndQueuedEventHistoryAreNotCoalesced() {
        val changes = StorageVolumeChanges(listOf(card()))
        assertFalse(changes.reconcile(listOf(card())))
        assertTrue(changes.observe(card(state = "unmounted")))
        assertTrue(changes.observe(card()))
        assertTrue(changes.reconcile(emptyList()))
        assertTrue(changes.observe(card()))
    }
    @Test fun changedVolumeIdentityInvalidatesEvenWithUnchangedMountState() {
        val changes = StorageVolumeChanges(listOf(card()))
        assertTrue(changes.reconcile(listOf(card("card-B"))))
        assertFalse(changes.observe(card("card-B")))
        assertTrue(changes.observe(card("card-C")))
    }
    @Test fun anonymousSecondaryEventsAreConservativeButPrimaryNullUuidIsStable() {
        val primary = StorageVolumeSample(true, null, "mounted")
        val changes = StorageVolumeChanges(listOf(primary, card(null)))
        assertFalse(changes.observe(primary))
        assertTrue(changes.observe(card(null)))
        assertTrue(changes.observe(card(null)))
        assertTrue(changes.reconcile(listOf(primary, card(null))))
    }
    @Test fun identityHistoryIsBoundedAndEvictedVolumeCannotReuseAnOldProof() {
        val changes = StorageVolumeChanges(emptyList())
        repeat(1000) { assertTrue(changes.observe(card("card-$it"))) }
        assertEquals(StorageVolumeChanges.MAX_VOLUMES, changes.retainedCount)
        assertTrue(changes.observe(card("card-0")))
        assertFalse(changes.observe(card("card-0")))
    }
    @Test fun oversizedOrUnidentifiableSnapshotsCannotLookUnchanged() {
        val snapshot = (0..StorageVolumeChanges.MAX_VOLUMES).map { card("card-$it") }
        val changes = StorageVolumeChanges(snapshot)
        assertEquals(StorageVolumeChanges.MAX_VOLUMES, changes.retainedCount)
        assertTrue(changes.reconcile(snapshot))
        assertTrue(changes.observe(card("x".repeat(129))))
    }
    @Test fun queuedCallbacksAfterCloseCannotReviveTrackingOrInvalidateAgain() {
        val changes = StorageVolumeChanges(listOf(card()))
        changes.close()
        assertFalse(changes.observe(card(state = "unmounted")))
        assertFalse(changes.reconcile(listOf(card("new-card"))))
        assertEquals(0, changes.retainedCount)
    }
}

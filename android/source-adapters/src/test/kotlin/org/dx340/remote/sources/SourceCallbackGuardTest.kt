package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class SourceCallbackGuardTest {
    @Test fun delayedQueueCallbackCannotChangeReplacementSessionAvailability() {
        val guard = SourceCallbackGuard()
        val old = guard.replace("apple", queueAvailable = true)
        val current = guard.replace("apple")
        var updates = 0
        assertFalse(guard.canReadQueue("apple"))
        guard.queueChanged(old, true) { updates++ }
        assertFalse(guard.canReadQueue("apple"))
        guard.queueChanged(current, true) { updates++ }
        guard.queueChanged(old, false) { updates++ }
        assertTrue(guard.canReadQueue("apple"))
        assertEquals(1, updates)
        assertTrue(guard.retire(current))
        assertFalse(guard.canReadQueue("apple"))
        guard.queueChanged(current, true) { updates++ }
        assertFalse(guard.canReadQueue("apple"))
        assertEquals(1, updates)
    }
    @Test fun queuePresenceChangesIndependentlyAndPermissionLossClearsAllFacts() {
        val guard = SourceCallbackGuard()
        val mango = guard.replace("mango", queueAvailable = true)
        val apple = guard.replace("apple", queueAvailable = true)
        guard.queueChanged(apple, false) {}
        assertFalse(guard.canReadQueue("apple"))
        assertTrue(guard.canReadQueue("mango"))
        // A non-null empty queue is readable; null means no exposed queue.
        guard.queueChanged(apple, true) {}
        assertTrue(guard.canReadQueue("apple"))
        guard.invalidateAll()
        guard.queueChanged(mango, true) {}; guard.queueChanged(apple, true) {}
        assertFalse(guard.canReadQueue("mango")); assertFalse(guard.canReadQueue("apple"))
        guard.replace("mango")
        assertFalse(guard.canReadQueue("mango"))
    }
    @Test fun closedRegistryCannotRestoreQueueCapabilityFromQueuedCallbacks() {
        val guard = SourceCallbackGuard()
        val lease = guard.replace("mango", queueAvailable = true)
        var updates = 0
        guard.close()
        guard.queueChanged(lease, true) { updates++ }
        assertFalse(guard.canReadQueue("mango"))
        assertFalse(guard.canReadQueue("missing"))
        assertEquals(0, updates)
    }
    @Test fun capabilityReadsDoNotPollSourceButExplicitRefreshRecoversMissedCallback() {
        val guard = SourceCallbackGuard()
        guard.replace("apple")
        var reads = 0
        val freshQueue = { reads++; true }
        repeat(100) { assertFalse(guard.canReadQueue("apple")) }
        assertEquals(0, reads)
        guard.refreshQueue("apple", freshQueue)
        repeat(100) { assertTrue(guard.canReadQueue("apple")) }
        assertEquals(1, reads)
        guard.refreshQueue("missing", freshQueue)
        guard.invalidateAll()
        guard.refreshQueue("apple", freshQueue)
        assertFalse(guard.canReadQueue("apple"))
        assertEquals(1, reads)
    }
    @Test fun delayedCallbacksAndDestructionCannotRetireReplacementSession() {
        val guard = SourceCallbackGuard()
        val old = guard.replace("mango")
        val current = guard.replace("mango")
        val updates = mutableListOf<String>()
        val queuedOldEvent = { guard.dispatch(old) { updates += "old" } }
        queuedOldEvent()
        assertFalse(guard.retire(old))
        guard.dispatch(current) { updates += "new" }
        assertEquals(listOf("new"), updates)
        assertTrue(guard.retire(current))
        assertFalse(guard.retire(current))
        guard.dispatch(current) { updates += "after destruction" }
        assertEquals(listOf("new"), updates)
    }
    @Test fun sourceOwnershipIsIndependent() {
        val guard = SourceCallbackGuard()
        val mango = guard.replace("mango"); val apple = guard.replace("apple")
        guard.replace("mango")
        var updates = 0
        assertFalse(guard.retire(mango))
        guard.dispatch(apple) { updates++ }
        assertEquals(1, updates)
    }
    @Test fun stoppingRegistryDropsQueuedCallbacksAndPreventsResurrection() {
        val guard = SourceCallbackGuard(); val lease = guard.replace("mango")
        var updates = 0
        val queued = { guard.dispatch(lease) { updates++ } }
        guard.close(); queued()
        assertEquals(0, updates)
        assertFalse(guard.retire(lease))
        assertThrows(IllegalStateException::class.java) { guard.replace("mango") }
    }
    @Test fun permissionLossInvalidatesAllOwnersAndRestorationRequiresNewLeases() {
        val guard = SourceCallbackGuard()
        val mango = guard.replace("mango"); val apple = guard.replace("apple")
        var updates = 0
        guard.invalidateAll()
        guard.dispatch(mango) { updates++ }; guard.dispatch(apple) { updates++ }
        assertFalse(guard.retire(mango)); assertFalse(guard.retire(apple))
        val restored = guard.replace("mango")
        guard.dispatch(mango) { updates++ }
        guard.dispatch(restored) { updates++ }
        assertEquals(1, updates)
    }
}

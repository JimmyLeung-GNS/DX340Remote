package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class SourceCallbackGuardTest {
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

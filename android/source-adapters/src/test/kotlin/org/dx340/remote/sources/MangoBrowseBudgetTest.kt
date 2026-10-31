package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class MangoBrowseBudgetTest {
    @Test fun ordinaryBrowseKeepsEightSecondsAndSdReloadFitsTheUnchangedPhoneReplyBudget() {
        val ordinary = MangoBrowseBudget(100)
        assertEquals(0L, ordinary.remainingMs(8100))
        assertTrue(ordinary.expired(8101))
        val reload = MangoBrowseBudget(100)
        reload.beginSdReload(600)
        assertEquals(9500L, reload.remainingMs(600))
        assertFalse(reload.expired(10100))
        assertTrue(reload.expired(10101))
        assertTrue(reload.expired(12100))
    }
    @Test fun repeatedReloadObservationsCannotRenewOrReviveAnExpiredOperation() {
        val reload = MangoBrowseBudget(0)
        reload.beginSdReload(7900)
        reload.beginSdReload(9999)
        assertEquals(1L, reload.remainingMs(9999))
        assertThrows(IllegalStateException::class.java) { reload.beginSdReload(10001) }
        val expired = MangoBrowseBudget(0)
        assertThrows(IllegalStateException::class.java) { expired.beginSdReload(8001) }
        assertTrue(expired.expired(8001))
    }
    @Test fun backwardClocksFailClosedAndLargeMonotonicValuesDoNotOverflow() {
        val budget = MangoBrowseBudget(400)
        assertEquals(-1L, budget.remainingMs(399))
        assertThrows(IllegalStateException::class.java) { budget.beginSdReload(399) }
        val nearLimit = MangoBrowseBudget(Long.MAX_VALUE - 1000)
        assertEquals(7000L, nearLimit.remainingMs(Long.MAX_VALUE))
        nearLimit.beginSdReload(Long.MAX_VALUE)
        assertEquals(9000L, nearLimit.remainingMs(Long.MAX_VALUE))
    }
}

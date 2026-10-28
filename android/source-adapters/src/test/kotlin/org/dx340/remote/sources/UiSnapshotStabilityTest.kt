package org.dx340.remote.sources

import org.junit.Assert.assertEquals
import org.junit.Test

class UiSnapshotStabilityTest {
    @Test fun matchingTextInANewWindowCannotBorrowOldSettlingTime() {
        val stable = UiSnapshotStability()
        assertEquals(0L, stable.observe(10, "folder-A", 100))
        assertEquals(450L, stable.observe(10, "folder-A", 550))
        assertEquals(0L, stable.observe(11, "folder-A", 600))
        assertEquals(200L, stable.observe(11, "folder-A", 800))
        assertEquals(0L, stable.observe(11, "folder-B", 900))
        assertEquals(0L, stable.observe(11, "folder-A", 1000))
        assertEquals(450L, stable.observe(11, "folder-A", 1450))
    }
    @Test fun backwardsClockResetsInsteadOfUsingAnOlderBaseline() {
        val stable = UiSnapshotStability()
        stable.observe(10, "menu", 100)
        assertEquals(300L, stable.observe(10, "menu", 400))
        assertEquals(0L, stable.observe(10, "menu", 350))
        assertEquals(450L, stable.observe(10, "menu", 800))
    }
}

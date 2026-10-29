package org.dx340.remote.sources

import org.junit.Assert.assertEquals
import org.junit.Test

class UiForegroundStartupTest {
    @Test fun aSlowFirstWindowWaitsWithoutLaunchingAgainAndHasAHardLimit() {
        val startup = UiForegroundStartup()
        assertEquals(UiForegroundStartup.Missing.LAUNCH, startup.missing(100))
        assertEquals(UiForegroundStartup.Missing.WAIT, startup.missing(410))
        assertEquals(UiForegroundStartup.Missing.WAIT, startup.missing(2099))
        assertEquals(UiForegroundStartup.Missing.FAIL, startup.missing(2100))
        assertEquals(UiForegroundStartup.Missing.FAIL, startup.missing(5000))
    }
    @Test fun lossAfterTheFirstRealWindowCannotRelaunchOrWait() {
        val startup = UiForegroundStartup()
        startup.observed()
        assertEquals(UiForegroundStartup.Missing.FAIL, startup.missing(100))
        val launched = UiForegroundStartup()
        launched.missing(100); launched.observed()
        assertEquals(UiForegroundStartup.Missing.FAIL, launched.missing(200))
    }
    @Test fun backwardTimeCannotExtendTheLaunchWait() {
        val startup = UiForegroundStartup()
        startup.missing(100)
        assertEquals(UiForegroundStartup.Missing.FAIL, startup.missing(99))
    }
}

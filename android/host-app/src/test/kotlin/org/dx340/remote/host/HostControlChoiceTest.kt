package org.dx340.remote.host

import org.junit.Assert.*
import org.junit.Test

class HostControlChoiceTest {
    private var enabled = false
    private val writes = mutableListOf<Boolean>()
    private val choice = HostControlChoice({ enabled }, { writes += it; enabled = it; true })

    @Test fun legacyOrNewInstallDoesNotInferEnablement() {
        assertEquals(HostControlChoice.Start.STOP, choice.resolve("RESTORE", true))
        assertEquals(HostControlChoice.Start.STOP, choice.resolve(null, true))
        assertTrue(writes.isEmpty())
    }
    @Test fun explicitEnableSurvivesReplacementWithoutReplayingPairOrProbe() {
        for (action in listOf("START", "PAIR", "PROBE")) {
            assertEquals(HostControlChoice.Start.valueOf(action), choice.resolve(action, true))
            writes.clear()
            assertEquals(HostControlChoice.Start.START, choice.resolve("RESTORE", true))
            assertEquals(HostControlChoice.Start.START, choice.resolve(null, true))
            assertTrue(writes.isEmpty())
        }
    }
    @Test fun stopWinsEvenWhenPermissionsMissingAndSuppressesQueuedRecovery() {
        choice.resolve("START", true)
        assertEquals(HostControlChoice.Start.STOP, choice.resolve("STOP", false))
        assertFalse(enabled)
        assertEquals(HostControlChoice.Start.STOP, choice.resolve("RESTORE", true))
        assertEquals(HostControlChoice.Start.STOP, choice.resolve(null, true))
    }
    @Test fun permissionLossPreventsStartWithoutInventingAUserStopOrEnable() {
        for (action in listOf("START", "PAIR", "PROBE", "RESTORE", null)) {
            assertEquals(HostControlChoice.Start.STOP, choice.resolve(action, false))
        }
        assertTrue(writes.isEmpty())
        enabled = true
        assertEquals(HostControlChoice.Start.STOP, choice.resolve("RESTORE", false))
        assertTrue(enabled)
    }
    @Test fun unknownActionsCannotEnableOrReplayWork() {
        enabled = true
        assertEquals(HostControlChoice.Start.STOP, choice.resolve("PLAY_ITEM", true))
        assertTrue(writes.isEmpty())
    }
    @Test fun enableWriteFailureDoesNotStartAnUnrecordedService() {
        val failed = HostControlChoice({ false }, { false })
        assertEquals(HostControlChoice.Start.STOP, failed.resolve("PAIR", true))
    }
}

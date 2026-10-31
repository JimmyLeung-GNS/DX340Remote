package org.dx340.remote.host

import org.junit.Assert.assertEquals
import org.junit.Test
import org.dx340.remote.host.HostRadioRecovery.Action.*
import org.dx340.remote.host.HostRadioRecovery.State.*

class HostRadioRecoveryTest {
    private val radio = HostRadioRecovery()

    @Test fun duplicateOnDoesNotReplaceAnActiveServer() {
        assertEquals(START, radio.update(ON, true))
        assertEquals(NONE, radio.update(ON, true))
    }
    @Test fun turningOffClosesOnceAndOnlyOnRestarts() {
        radio.update(ON, true)
        assertEquals(STOP, radio.update(TURNING_OFF, true))
        assertEquals(NONE, radio.update(OFF, true))
        assertEquals(NONE, radio.update(TURNING_ON, true))
        assertEquals(START, radio.update(ON, true))
    }
    @Test fun missingTurningOffStillClosesAtOff() {
        radio.update(ON, true)
        assertEquals(STOP, radio.update(OFF, true))
        assertEquals(START, radio.update(ON, true))
    }
    @Test fun enableWhileOffWaitsForOn() {
        assertEquals(NONE, radio.update(OFF, true))
        assertEquals(NONE, radio.update(TURNING_ON, true))
        assertEquals(START, radio.update(ON, true))
    }
    @Test fun stoppedChoicePreventsRecoveryAndClosesExistingTransport() {
        radio.update(ON, true)
        assertEquals(STOP, radio.update(ON, false))
        assertEquals(NONE, radio.update(OFF, false))
        assertEquals(NONE, radio.update(ON, false))
    }
    @Test fun failedStartCanBeRetriedByAnotherExplicitEnable() {
        radio.update(ON, true); radio.startFailed()
        assertEquals(START, radio.update(ON, true))
    }
    @Test fun unknownStateCannotOpenOrCloseTransport() {
        assertEquals(NONE, radio.update(UNKNOWN, true))
        assertEquals(START, radio.update(ON, true))
        assertEquals(NONE, radio.update(UNKNOWN, false))
        assertEquals(STOP, radio.update(OFF, true))
    }
}

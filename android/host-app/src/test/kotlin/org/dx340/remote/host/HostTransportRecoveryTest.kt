package org.dx340.remote.host

import org.junit.Assert.*
import org.junit.Test

class HostTransportRecoveryTest {
    @Test fun onlyTwoAutomaticRecoveriesFitAWindow() {
        val r = HostTransportRecovery()
        assertTrue(r.request(0, true, true, true)); assertTrue(r.request(1, true, true, true))
        assertFalse(r.request(2, true, true, true)); assertFalse(r.request(59999, true, true, true))
    }
    @Test fun exactWindowBoundaryReleasesOnlyExpiredAttempts() {
        val r = HostTransportRecovery()
        r.request(0, true, true, true); r.request(1000, true, true, true)
        assertTrue(r.request(60000, true, true, true)); assertFalse(r.request(60001, true, true, true))
        assertTrue(r.request(61000, true, true, true))
    }
    @Test fun stopPermissionLossAndRadioOffCannotSpendRecoveryBudget() {
        for (state in listOf(Triple(false,true,true),Triple(true,false,true),Triple(true,true,false),Triple(false,false,false))) {
            val r = HostTransportRecovery()
            repeat(5) { assertFalse(r.request(it.toLong(), state.first, state.second, state.third)) }
            assertTrue(r.request(10, true, true, true)); assertTrue(r.request(11, true, true, true))
        }
    }
    @Test fun regressedClockDoesNotReopenTheBudget() {
        val r = HostTransportRecovery()
        assertTrue(r.request(100, true, true, true)); assertTrue(r.request(101, true, true, true))
        assertFalse(r.request(99, true, true, true)); assertFalse(r.request(102, true, true, true))
    }
    @Test fun explicitOwnerRestartCanResetAnExhaustedBudget() {
        val r = HostTransportRecovery()
        r.request(100, true, true, true); r.request(101, true, true, true)
        assertFalse(r.request(102, true, true, true)); r.explicitRestart()
        assertTrue(r.request(103, true, true, true))
    }
    @Test fun negativeClockCannotCreateARecovery() {
        val r = HostTransportRecovery()
        assertFalse(r.request(-1, true, true, true)); assertTrue(r.request(0, true, true, true))
    }
}

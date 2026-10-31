package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test
import org.dx340.remote.protocol.GattProcedureGate.Operation.*
import org.dx340.remote.protocol.GattProcedureGate.Completion.*

class GattProcedureGateTest {
    @Test fun initialDiscoveryChangeDrainsOldCallbackBeforeFreshDiscovery() {
        val gate = GattProcedureGate()
        assertTrue(gate.begin(DISCOVER)); assertFalse(gate.changed()); assertFalse(gate.changed())
        assertEquals(REDISCOVER, gate.complete(DISCOVER))
        assertTrue(gate.begin(DISCOVER)); assertEquals(CONTINUE, gate.complete(DISCOVER))
    }
    @Test fun idleInvalidationStartsOneFreshSerializedSetup() {
        val gate = GattProcedureGate(); assertTrue(gate.changed())
        for (operation in listOf(DISCOVER, MTU, SUBSCRIBE)) {
            assertTrue(gate.begin(operation)); assertEquals(CONTINUE, gate.complete(operation))
        }
        assertFalse(gate.busy)
    }
    @Test fun invalidatedMtuOrSubscriptionCannotAdvanceOldSetupToReady() {
        for (operation in listOf(MTU, SUBSCRIBE)) {
            val gate = GattProcedureGate(); assertTrue(gate.begin(operation)); assertFalse(gate.changed())
            assertEquals(REDISCOVER, gate.complete(operation))
        }
    }
    @Test fun pendingWriteDrainsToRediscoveryRatherThanAdvancingTheOldWriteQueue() {
        val gate = GattProcedureGate(); gate.begin(WRITE); assertFalse(gate.changed())
        assertFalse(gate.begin(DISCOVER)); assertEquals(REDISCOVER, gate.complete(WRITE))
        assertTrue(gate.begin(DISCOVER)); assertEquals(CONTINUE, gate.complete(DISCOVER))
    }
    @Test fun unrelatedCallbacksCannotReleaseTheProcedureOrConsumeInvalidation() {
        val gate = GattProcedureGate(); gate.begin(DISCOVER); gate.changed()
        assertEquals(IGNORED, gate.complete(WRITE)); assertEquals(IGNORED, gate.complete(MTU))
        assertTrue(gate.busy); assertEquals(REDISCOVER, gate.complete(DISCOVER))
    }
    @Test fun lossClearsOldProcedureAndInvalidationWithoutAQueuedReplay() {
        val gate = GattProcedureGate(); gate.begin(WRITE); gate.changed(); gate.clear()
        assertEquals(IGNORED, gate.complete(WRITE)); assertFalse(gate.busy)
        assertTrue(gate.begin(DISCOVER)); assertEquals(CONTINUE, gate.complete(DISCOVER))
    }
    @Test fun overlappingProcedureCannotReplaceTheCurrentOne() {
        val gate = GattProcedureGate(); gate.begin(MTU)
        assertFalse(gate.begin(SUBSCRIBE)); assertEquals(IGNORED, gate.complete(SUBSCRIBE))
        assertEquals(CONTINUE, gate.complete(MTU))
    }
    @Test fun anotherChangeDuringReplacementDiscoveryRequiresAnotherFreshResult() {
        val gate = GattProcedureGate(); gate.begin(DISCOVER); gate.changed()
        assertEquals(REDISCOVER, gate.complete(DISCOVER)); gate.begin(DISCOVER)
        gate.changed(); gate.changed(); assertEquals(REDISCOVER, gate.complete(DISCOVER))
        gate.begin(DISCOVER); assertEquals(CONTINUE, gate.complete(DISCOVER))
    }
}

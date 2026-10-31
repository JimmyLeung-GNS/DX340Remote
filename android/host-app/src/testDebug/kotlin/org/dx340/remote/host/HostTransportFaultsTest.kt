package org.dx340.remote.host

import org.dx340.remote.protocol.PeerIndicationQueue
import org.junit.Assert.*
import org.junit.Test

class HostTransportFaultsTest {
    private var now = 100L
    private val faults = HostTransportFaults { now }
    private val link = Any()

    @Test fun unarmedAndConsumedCallbacksAreNeverIgnored() {
        assertFalse(faults.ignoreCompletion(link, true))
        assertTrue(faults.arm(link))
        assertTrue(faults.ignoreCompletion(link, true))
        assertFalse(faults.ignoreCompletion(link, true))
    }
    @Test fun equalValuedDifferentLinkCannotSpendTheArm() {
        data class EqualLink(val id: Int)
        val first = EqualLink(1); val replacement = EqualLink(1)
        assertTrue(faults.arm(first))
        assertFalse(faults.ignoreCompletion(replacement, true))
        assertTrue(faults.ignoreCompletion(first, true))
    }
    @Test fun unsuccessfulCompletionDoesNotHideStackFailure() {
        assertTrue(faults.arm(link))
        assertFalse(faults.ignoreCompletion(link, false))
        assertTrue(faults.ignoreCompletion(link, true))
    }
    @Test fun expiresAtExactlyTwentySeconds() {
        assertTrue(faults.arm(link)); now += 20000
        assertFalse(faults.ignoreCompletion(link, true))
        assertTrue(faults.arm(link)); now += 19999
        assertTrue(faults.ignoreCompletion(link, true))
    }
    @Test fun repeatedArmCannotExtendDeadlineOrChangeLink() {
        assertTrue(faults.arm(link)); now += 19999
        assertFalse(faults.arm(Any())); now++
        assertFalse(faults.ignoreCompletion(link, true))
    }
    @Test fun lossClearsArmBeforeAnotherConnection() {
        assertTrue(faults.arm(link)); faults.clear()
        assertFalse(faults.ignoreCompletion(link, true))
        val replacement = Any(); assertTrue(faults.arm(replacement))
        assertFalse(faults.ignoreCompletion(link, true))
        assertTrue(faults.ignoreCompletion(replacement, true))
    }
    @Test fun invalidAndRegressedClockFailClosed() {
        now = -1; assertFalse(faults.arm(link))
        now = 100; assertTrue(faults.arm(link)); now = 99
        assertFalse(faults.ignoreCompletion(link, true))
        now = 100; assertTrue(faults.arm(link)); now = 99
        assertFalse(faults.arm(link))
        assertFalse(faults.ignoreCompletion(link, true))
    }
    @Test fun highValidClockDoesNotOverflowDeadline() {
        now = Long.MAX_VALUE - 19999; assertTrue(faults.arm(link))
        now = Long.MAX_VALUE; assertTrue(faults.ignoreCompletion(link, true))
    }
    @Test fun ignoredCallbackReachesExistingDeadlineWithoutPumpingAnotherFrame() {
        val timers = mutableListOf<() -> Unit>()
        val submitted = mutableListOf<ByteArray>()
        val failures = mutableListOf<String>()
        val queue = PeerIndicationQueue<Any>(
            schedule = { delay, action -> assertEquals(10000L, delay); timers.add(action); {} },
            submit = { _, bytes -> submitted.add(bytes); null },
            failed = { _, reason -> failures.add(reason); faults.clear() })
        assertTrue(queue.open(link)); assertTrue(faults.arm(link))
        queue.enqueue(link, listOf(byteArrayOf(1), byteArrayOf(2)))
        if (!faults.ignoreCompletion(link, true)) queue.complete(link)
        assertEquals(1, submitted.size); assertEquals(2, queue.pendingCount)
        timers.single().invoke()
        assertEquals(listOf("INDICATION_TIMEOUT"), failures)
        assertEquals(1, submitted.size); assertEquals(0, queue.pendingCount)
        // A freshly opened connection proceeds normally; old timer cannot fail it.
        val replacement = Any(); assertTrue(queue.open(replacement))
        queue.enqueue(replacement, listOf(byteArrayOf(3)))
        timers.first().invoke(); assertEquals(1, failures.size)
        assertFalse(faults.ignoreCompletion(replacement, true)); queue.complete(replacement)
        assertEquals(0, queue.pendingCount); assertEquals(2, submitted.size)
    }
}

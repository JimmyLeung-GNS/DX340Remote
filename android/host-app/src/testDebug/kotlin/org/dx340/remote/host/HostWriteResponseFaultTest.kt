package org.dx340.remote.host

import org.junit.Assert.*
import org.junit.Test

class HostWriteResponseFaultTest {
    private var now = 100L
    private data class Timer(val delay: Long, val fire: () -> Unit, var cancelled: Boolean = false)
    private val timers = mutableListOf<Timer>()
    private val faults = HostTransportFaults({ now }, schedule = { delay, fire ->
        val timer = Timer(delay, fire); timers.add(timer)
        val cancel: () -> Unit = { timer.cancelled = true }; cancel
    })
    private val link = Any()

    @Test fun noSchedulerCannotCreateAnUnboundedWriteHold() {
        val noScheduler = HostTransportFaults({ now })
        assertFalse(noScheduler.armWrite(link))
        assertFalse(noScheduler.withholdWriteResponse(link) { fail("Must not expire an unarmed hold") })
        assertTrue(noScheduler.arm(link)); assertTrue(noScheduler.ignoreCompletion(link, true))
    }
    @Test fun exactlyOneCurrentLinkWriteIsHeldUntilTheBoundedExpiry() {
        data class Link(val id: Int)
        val current = Link(1); val equalOther = Link(1); var closed = 0
        assertTrue(faults.armWrite(current))
        assertFalse(faults.withholdWriteResponse(equalOther) { fail("Wrong link") })
        assertTrue(faults.withholdWriteResponse(current) { closed++ })
        assertFalse(faults.withholdWriteResponse(current) { fail("Second write") })
        assertFalse(faults.armWrite(Any())); assertFalse(faults.arm(Any()))
        assertEquals(1, timers.size); assertEquals(5000L, timers.single().delay); assertEquals(0, closed)
        timers.single().fire(); timers.single().fire(); assertEquals(1, closed)
    }
    @Test fun clearedTimerCannotCloseAReplacementEvenWhenItsActionArrivesLate() {
        val closed = mutableListOf<String>()
        assertTrue(faults.armWrite(link)); assertTrue(faults.withholdWriteResponse(link) { closed.add("old") })
        faults.clear(); assertTrue(timers[0].cancelled)
        val replacement = Any()
        assertTrue(faults.armWrite(replacement)); assertTrue(faults.withholdWriteResponse(replacement) { closed.add("new") })
        timers[0].fire(); assertTrue(closed.isEmpty())
        timers[1].fire(); assertEquals(listOf("new"), closed)
    }
    @Test fun callbackAndWriteFaultModesCannotConsumeOrReplaceEachOther() {
        assertTrue(faults.arm(link)); assertFalse(faults.armWrite(link))
        assertFalse(faults.withholdWriteResponse(link) { fail("Wrong fault mode") })
        assertTrue(faults.ignoreCompletion(link, true))
        assertTrue(faults.armWrite(link)); assertFalse(faults.arm(link))
        assertFalse(faults.ignoreCompletion(link, true))
        assertTrue(faults.withholdWriteResponse(link) {})
        assertFalse(faults.ignoreCompletion(link, true)); faults.clear()
    }
    @Test fun expiredOrRegressedWriteArmDoesNotWithholdAnyResponse() {
        assertTrue(faults.armWrite(link)); now += 20000
        assertFalse(faults.withholdWriteResponse(link) { fail("Expired") }); assertTrue(timers.isEmpty())
        assertTrue(faults.armWrite(link)); now--
        assertFalse(faults.withholdWriteResponse(link) { fail("Regressed clock") }); assertTrue(timers.isEmpty())
    }
    @Test fun clearInvalidatesTheGenerationBeforeInvokingItsCancellation() {
        var closed = 0
        val reentrant = HostTransportFaults({ now }, schedule = { _, fire -> { fire() } })
        assertTrue(reentrant.armWrite(link)); assertTrue(reentrant.withholdWriteResponse(link) { closed++ })
        reentrant.clear(); assertEquals(0, closed)
        assertTrue(reentrant.arm(Any()))
    }
}

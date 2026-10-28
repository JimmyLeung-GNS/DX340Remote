package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class IndicationQueueTest {
    private class Rig(limit: Int = 12000) {
        data class Task(val time: Long, val run: () -> Unit, var cancelled: Boolean = false)
        var now = 0L
        var submitError: String? = null
        val tasks = mutableListOf<Task>()
        val sent = mutableListOf<Int>()
        val failures = mutableListOf<String>()
        val pendingAtFailure = mutableListOf<Int>()
        val acceptedAtFailure = mutableListOf<Boolean>()
        val queue = IndicationQueue({ delay, action ->
            val task = Task(now + delay, action); tasks += task
            val cancel: () -> Unit = { task.cancelled = true }; cancel
        }, { sent += it.single().toInt(); submitError }, ::failure, limit)
        private fun failure(reason: String) {
            failures += reason
            pendingAtFailure += queue.pendingCount
            acceptedAtFailure += queue.enqueue(emptyList())
        }
        fun frames(vararg bytes: Int) = bytes.map { byteArrayOf(it.toByte()) }
        fun advance(milliseconds: Long) {
            now += milliseconds
            tasks.filter { !it.cancelled && it.time <= now }.toList().forEach { it.cancelled = true; it.run() }
        }
    }

    @Test fun serializesFramesAndCancelsAcknowledgedDeadlines() {
        val r = Rig(); r.queue.open()
        assertTrue(r.queue.enqueue(r.frames(1, 2, 3)))
        assertEquals(listOf(1), r.sent); assertEquals(3, r.queue.pendingCount)
        r.queue.complete(); assertEquals(listOf(1, 2), r.sent)
        r.queue.complete(); r.queue.complete()
        r.advance(10000)
        assertEquals(listOf(1, 2, 3), r.sent)
        assertEquals(0, r.queue.pendingCount); assertTrue(r.failures.isEmpty())
        assertTrue(r.tasks.all { it.cancelled })
    }

    @Test fun missingCompletionClosesAndDropsAllPendingFramesWithoutReplay() {
        val r = Rig(); r.queue.open(); r.queue.enqueue(r.frames(1, 2))
        r.queue.enqueue(r.frames(3), lowPriority = true)
        r.advance(9999); assertTrue(r.failures.isEmpty())
        r.advance(1)
        assertEquals(listOf("INDICATION_TIMEOUT"), r.failures)
        assertEquals(0, r.queue.pendingCount)
        r.queue.complete(); assertFalse(r.queue.enqueue(r.frames(4)))
        assertEquals(listOf(1), r.sent)
        r.queue.open(); r.queue.enqueue(r.frames(5))
        assertEquals(listOf(1, 5), r.sent)
    }

    @Test fun disconnectAndOldQueuedTimeoutCannotTerminateANewTransfer() {
        val r = Rig(); r.queue.open(); r.queue.enqueue(r.frames(1, 2))
        val oldTimeout = r.tasks.single().run
        r.queue.close(); r.queue.complete()
        r.queue.open(); r.queue.enqueue(r.frames(3))
        oldTimeout() // Even an already dequeued/cancelled timer must be harmless.
        assertEquals(1, r.queue.pendingCount); assertTrue(r.failures.isEmpty())
        r.queue.complete(); r.advance(10000)
        assertEquals(listOf(1, 3), r.sent); assertTrue(r.failures.isEmpty())
    }

    @Test fun callbackErrorAndImmediateSubmitFailureDropQueuesOnce() {
        val r = Rig(); r.queue.open(); r.queue.enqueue(r.frames(1, 2))
        r.queue.complete("INDICATION_FAILED:257"); r.queue.complete()
        assertEquals(listOf("INDICATION_FAILED:257"), r.failures)
        assertEquals(listOf(1), r.sent); assertEquals(0, r.queue.pendingCount)
        r.queue.open(); r.submitError = "SEND_FAILED:5"
        assertFalse(r.queue.enqueue(r.frames(3, 4)))
        r.advance(10000)
        assertEquals(listOf("INDICATION_FAILED:257", "SEND_FAILED:5"), r.failures)
        assertEquals(listOf(1, 3), r.sent); assertEquals(0, r.queue.pendingCount)
    }

    @Test fun controlsPreemptArtworkBetweenConfirmedFramesWithoutDuplicatingEither() {
        val r = Rig(); r.queue.open()
        r.queue.enqueue(r.frames(1, 2), lowPriority = true)
        r.queue.enqueue(r.frames(3, 4))
        repeat(4) { r.queue.complete() }
        assertEquals(listOf(1, 3, 4, 2), r.sent)
        assertEquals(0, r.queue.pendingCount)
    }

    @Test fun overflowIncludesInFlightFrameAndRevokesQueueBeforeFailureCallback() {
        val r = Rig(limit = 2); r.queue.open(); r.queue.enqueue(r.frames(1, 2))
        assertFalse(r.queue.enqueue(r.frames(3)))
        r.queue.complete(); r.advance(10000)
        assertEquals(listOf("OUTGOING_LIMIT"), r.failures)
        assertEquals(listOf(0), r.pendingAtFailure)
        assertEquals(listOf(false), r.acceptedAtFailure)
        assertEquals(listOf(1), r.sent); assertEquals(0, r.queue.pendingCount)
        assertFalse(r.queue.enqueue(r.frames(4)))
    }

    @Test fun sustainedAcknowledgementsDoNotAccumulateActiveTimeouts() {
        val r = Rig(); r.queue.open()
        repeat(500) { r.queue.enqueue(r.frames(1)); r.queue.complete() }
        assertEquals(500, r.sent.size)
        assertTrue(r.tasks.none { !it.cancelled })
        assertTrue(r.failures.isEmpty())
    }
}

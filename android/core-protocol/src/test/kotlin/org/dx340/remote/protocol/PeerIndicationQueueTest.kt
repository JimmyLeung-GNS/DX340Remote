package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class PeerIndicationQueueTest {
    private class Rig(limit: Int = 12000) {
        data class Task(val at: Long, val run: () -> Unit, var cancelled: Boolean = false)
        var now = 0L
        val tasks = mutableListOf<Task>()
        val sent = mutableListOf<Pair<String, Int>>()
        val failures = mutableListOf<Pair<String, String>>()
        val queue = PeerIndicationQueue<String>({ delay, run ->
            val task = Task(now + delay, run); tasks += task
            val cancel: () -> Unit = { task.cancelled = true }; cancel
        }, { peer, bytes -> sent += peer to bytes.single().toInt(); null }, { peer, error -> failures += peer to error }, frameLimit = limit)
        fun send(peer: String, vararg bytes: Int, bulk: Boolean = false) = queue.enqueue(peer, bytes.map { byteArrayOf(it.toByte()) }, bulk)
        fun advance(ms: Long) { now += ms; tasks.filter { !it.cancelled && it.at <= now }.toList().forEach { it.cancelled = true; it.run() } }
    }
    @Test fun serializesOsCallsAcrossPeersAndPrioritizesControlOverRemainingArtwork() {
        val r = Rig(); assertTrue(r.queue.open("A")); assertTrue(r.queue.open("B")); assertFalse(r.queue.open("C"))
        r.send("A", 1, 2, bulk = true); r.send("B", 3)
        assertEquals(listOf("A" to 1), r.sent)
        r.queue.complete("B") // Wrong-device callback cannot release A's OS operation.
        assertEquals(listOf("A" to 1), r.sent)
        r.queue.complete("A"); r.queue.complete("B"); r.queue.complete("A")
        assertEquals(listOf("A" to 1, "B" to 3, "A" to 2), r.sent)
        assertEquals(0, r.queue.pendingCount)
    }
    @Test fun handoffDropsUnsentOldMetadataButAllowsOneSubmittedValueToDrain() {
        val r = Rig(); r.queue.open("A"); r.queue.open("B")
        r.send("A", 1, 2, 3, bulk = true)
        r.queue.discardQueued("A"); r.send("A", 9); r.send("B", 5)
        r.queue.complete("A")
        assertEquals(listOf("A" to 1, "A" to 9), r.sent)
        r.queue.close("A") // Old client receives CONTROL_MOVED and disconnects.
        assertEquals("A", r.queue.inFlightPeer)
        r.queue.complete("B"); assertEquals(2, r.sent.size)
        r.queue.complete("A"); r.queue.complete("B")
        assertEquals(listOf("A" to 1, "A" to 9, "B" to 5), r.sent)
        assertTrue(r.failures.isEmpty())
    }
    @Test fun disconnectedPeersMissingCallbackStopsAllSubmissionsUntilTransportReset() {
        val r = Rig(); r.queue.open("A"); r.queue.open("B")
        r.send("A", 1); r.queue.close("A"); r.send("B", 2)
        r.advance(9999); assertEquals(listOf("A" to 1), r.sent)
        r.advance(1); assertEquals(listOf("A" to 1), r.sent)
        assertEquals(listOf("A" to "INDICATION_TIMEOUT"), r.failures)
        assertEquals(0, r.queue.pendingCount)
        r.queue.complete("A"); assertNull(r.queue.inFlightPeer)
        assertFalse(r.send("B", 3))
    }
    @Test fun overflowClosesOnlyAffectedPeerButMissingCompletionStopsWholeQueue() {
        val r = Rig(limit = 2); r.queue.open("A"); r.queue.open("B")
        r.send("A", 1, 2)
        assertFalse(r.send("B", 3))
        assertEquals(listOf("B" to "OUTGOING_LIMIT"), r.failures)
        r.queue.complete("A"); r.queue.complete("A")
        assertEquals(listOf("A" to 1, "A" to 2), r.sent)
        r.queue.open("B"); r.send("A", 4); r.send("B", 5)
        r.advance(10000)
        assertEquals("A" to "INDICATION_TIMEOUT", r.failures.last())
        assertEquals("A" to 4, r.sent.last())
        assertNull(r.queue.inFlightPeer)
        r.queue.complete("B")
    }
    @Test fun stoppedQueuesAndStaleTimersCannotCloseAReopenedPeer() {
        val r = Rig(); r.queue.open("A"); r.send("A", 1)
        val oldTimeout = r.tasks.single().run
        r.queue.stop(); r.queue.open("B"); r.send("B", 2)
        oldTimeout(); assertEquals("B", r.queue.inFlightPeer)
        assertTrue(r.failures.isEmpty()); r.queue.complete("B")
    }
    @Test fun identicalFrameIdsOnDifferentConnectionsCannotMixPayloads() {
        val decoded = mutableMapOf<String, ByteArray>()
        val assemblers = mapOf("A" to Reassembler { 0 }, "B" to Reassembler { 0 })
        val queue = PeerIndicationQueue<String>({ _, _ -> {} }, { peer, bytes ->
            assemblers.getValue(peer).accept(bytes)?.let { decoded[peer] = it }; null
        }, { _, error -> error(error) })
        queue.open("A"); queue.open("B")
        val a = "Mango 日本語".toByteArray(); val b = "Apple playlist".toByteArray()
        queue.enqueue("A", Wire.frames(a, 20, 7), true); queue.enqueue("B", Wire.frames(b, 20, 7))
        while (queue.inFlightPeer != null) queue.complete(queue.inFlightPeer!!)
        assertArrayEquals(a, decoded["A"]); assertArrayEquals(b, decoded["B"])
    }
}

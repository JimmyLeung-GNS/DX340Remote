package org.dx340.remote.protocol

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class AcknowledgedWriteQueueTest {
    @Test fun sharedFixturesCoverFramingFifoCancellationAndAtomicBounds() {
        val cases = JSONArray(javaClass.getResource("/acknowledged-writes.json")!!.readText())
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i); val q = AcknowledgedWriteQueue<String>(c.getInt("capacity"))
            val steps = c.getJSONArray("steps")
            for (n in 0 until steps.length()) {
                val s = steps.getJSONObject(n); val label = c.getString("name") + " step " + n
                when (s.getString("action")) {
                    "enqueue" -> {
                        val arrays = s.getJSONArray("frames")
                        val frames = (0 until arrays.length()).map { index -> arrays.getJSONArray(index).let { a -> ByteArray(a.length()) { a.getInt(it).toByte() } } }
                        var rejected = false
                        try { q.enqueue(frames, s.getString("tag"), if (s.has("submission") && !s.isNull("submission")) s.getString("submission") else null) } catch (_: IllegalArgumentException) { rejected = true }
                        assertEquals(label, s.optBoolean("rejected"), rejected)
                    }
                    "next" -> if (s.isNull("bytes")) assertNull(label, q.next()) else {
                        val a = s.getJSONArray("bytes"); assertArrayEquals(label, ByteArray(a.length()) { a.getInt(it).toByte() }, q.next())
                    }
                    "ack" -> assertEquals(label, if (s.isNull("tag")) null else s.getString("tag"), q.acknowledge())
                    "submitted" -> assertEquals(label, if (s.isNull("tag")) null else s.getString("tag"), q.submitted())
                    "clear" -> q.clear()
                    else -> fail("Unknown fixture step")
                }
            }
            assertFalse(c.getString("name"), q.busy); assertEquals(0, q.pendingCount)
        }
    }

    @Test fun firstSubmissionAndLastAcknowledgementKeepDifferentQueueWaits() {
        var clock = 0L; val journal = RequestJournal { clock }; val q = AcknowledgedWriteQueue<String>()
        journal.begin("a", "GET_STATE", "mango"); q.enqueue(listOf(byteArrayOf(1), byteArrayOf(2)), "a", "a")
        q.next(); clock = 1; journal.writeSubmitted(q.submitted()!!)
        clock = 10; journal.begin("b", "GET_STATE", "mango"); q.enqueue(listOf(byteArrayOf(3)), "b", "b")
        clock = 20; assertNull(q.acknowledge()); q.next(); assertNull(q.submitted())
        clock = 40; journal.writeAcknowledged(q.acknowledge()!!)
        q.next(); clock = 41; journal.writeSubmitted(q.submitted()!!)
        clock = 70; journal.writeAcknowledged(q.acknowledge()!!)
        val rows = JSONObjectView.rows(journal.report())
        assertEquals(1L, rows[0]["firstWriteSubmittedAfterMs"]); assertEquals(40L, rows[0]["writeAcknowledgedAfterMs"])
        assertEquals(31L, rows[1]["firstWriteSubmittedAfterMs"]); assertEquals(60L, rows[1]["writeAcknowledgedAfterMs"])
        assertTrue(rows.all { it["result"] == "pending" && it["terminalApplied"] == false })
    }

    @Test fun queuedMultiFrameAcknowledgementsUseEachRequestsOwnStartAndDoNotCompletePlayback() {
        var clock = 0L; val journal = RequestJournal { clock }; val q = AcknowledgedWriteQueue<String>()
        journal.begin("a", "NEXT", "mango"); q.enqueue(listOf(byteArrayOf(1), byteArrayOf(2)), "a")
        clock = 10; journal.begin("b", "GET_STATE", "mango"); q.enqueue(listOf(byteArrayOf(3)), "b")
        val before = journal.report()
        q.next(); clock = 20; assertNull(q.acknowledge())
        q.next(); clock = 40; journal.writeAcknowledged(q.acknowledge()!!)
        q.next(); clock = 70; journal.writeAcknowledged(q.acknowledge()!!)
        val rows = JSONObjectView.rows(journal.report())
        assertEquals(40L, rows[0]["writeAcknowledgedAfterMs"]); assertEquals(60L, rows[1]["writeAcknowledgedAfterMs"])
        assertTrue(rows.all { it["result"] == "pending" && it["accepted"] == false && it["terminalApplied"] == false })
        assertTrue(JSONObjectView.rows(before).all { it["writeAcknowledgedAfterMs"] == null })
    }

    private object JSONObjectView {
        @Suppress("UNCHECKED_CAST") fun rows(report: Map<String, Any?>) = report["requests"] as List<Map<String, Any?>>
    }
}

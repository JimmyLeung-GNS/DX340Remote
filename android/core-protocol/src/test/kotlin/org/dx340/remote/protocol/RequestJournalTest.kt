package org.dx340.remote.protocol

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RequestJournalTest {
    private fun normalized(v: Any?): Any? = when (v) {
        null, JSONObject.NULL -> null
        is JSONObject -> v.keys().asSequence().sorted().associateWith { normalized(v.get(it)) }
        is JSONArray -> (0 until v.length()).map { normalized(v.get(it)) }
        is Number -> v.toLong()
        is Map<*, *> -> v.keys.map { it as String }.sorted().associateWith { normalized(v[it]) }
        else -> v
    }
    @Test fun sharedFixturesPreserveFirstReceiptsAndBoundsWithoutMetadata() {
        val cases = JSONArray(javaClass.getResource("/request-journal.json")!!.readText())
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i); var clock = 0L
            val journal = RequestJournal(c.getInt("capacity")) { clock }
            val steps = c.getJSONArray("steps")
            for (n in 0 until steps.length()) {
                val s = steps.getJSONObject(n); clock = s.getLong("at")
                when (s.getString("action")) {
                    "begin" -> journal.begin(s.getString("id"), s.getString("operation"), s.getString("source"))
                    "reply" -> journal.reply(s.getString("id"), s.getString("type"), s.optString("outcome"), s.optString("error"))
                    "apply" -> journal.markTerminalApplied(s.getString("id"))
                    "writeAck" -> journal.writeAcknowledged(s.getString("id"))
                    "writeSubmitted" -> journal.writeSubmitted(s.getString("id"))
                    "finish" -> journal.finish(s.getString("id"), s.getString("reason"))
                    "abandon" -> journal.abandon(s.getString("reason"))
                    "phase" -> journal.phase(s.getString("phase"))
                    else -> fail("Unknown fixture action")
                }
            }
            assertEquals(c.getString("name"), normalized(c.getJSONObject("expected")), normalized(journal.report()))
        }
    }
    @Test fun sustainedRequestsAreBoundedAndPreviouslyReturnedSnapshotsStayImmutable() {
        var clock = 100L; val journal = RequestJournal(128) { clock }
        journal.begin("first", "NEXT", "mango")
        val before = JSONObject(journal.report()).toString()
        repeat(10000) { clock++; journal.begin("read-$it", "GET_STATE", "mango"); journal.reply("read-$it", "RESULT") }
        val r = JSONObject(journal.report()); assertEquals(128, r.getJSONArray("requests").length()); assertEquals(9873L, r.getLong("evictedRequests"))
        assertNull(journal.reply("first", "RESULT")); assertEquals("pending", JSONObject(before).getJSONArray("requests").getJSONObject(0).getString("result"))
    }
    @Test fun changedHostRepliesMustBeRejectedBeforeTheyCanBecomeAReceipt() {
        var clock = 0L; val journal = RequestJournal { clock }; val session = ControllerSession()
        session.hello("A"); session.approve("A"); session.register("skip", "NEXT"); journal.begin("skip", "NEXT", "mango")
        clock = 5
        assertEquals(HostReplyDecision.RECONNECT, HostReplyGate.classify("ERROR", session.pending["skip"], "A", "B", true))
        journal.abandon(); session.lost()
        clock = 8
        assertEquals(HostReplyDecision.IGNORE, HostReplyGate.classify("RESULT", session.pending["skip"], "", "A", false))
        assertNull(journal.reply("skip", "RESULT"))
        val row = JSONObject(journal.report()).getJSONArray("requests").getJSONObject(0)
        assertEquals("connection_lost", row.getString("result")); assertEquals(5, row.getInt("finishedAfterMs")); assertFalse(session.authorized)
    }
}

package org.dx340.remote.protocol

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class HostReplyGateTest {
    @Test fun changedHostsCannotHideBehindErrorOrAcceptedAndUnmatchedOldRepliesAreIgnored() {
        val cases = JSONArray(javaClass.getResource("/host-reply-gate.json")!!.readText())
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val result = HostReplyGate.classify(c.getString("type"), if (c.isNull("operation")) null else c.getString("operation"), c.getString("expected"), c.getString("reply"), c.getBoolean("authorized"))
            assertEquals(c.getString("name"), c.getString("result"), result.name.lowercase())
        }
    }
    @Test fun restartDiscardsQueuedMutationAndLateRepliesCannotReviveItsOldGrant() {
        val session = ControllerSession()
        session.hello("host-A"); session.approve("host-A")
        session.register("read-old", "GET_STATE"); session.register("skip-old", "NEXT")
        val decision = HostReplyGate.classify("ERROR", session.pending["read-old"], session.hostEpoch, "host-B", session.authorized)
        assertEquals(HostReplyDecision.RECONNECT, decision)
        session.lost()
        assertFalse(session.authorized); assertTrue(session.pending.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { session.register("skip-new", "NEXT") }
        session.hello("host-B")
        assertEquals(HostReplyDecision.IGNORE, HostReplyGate.classify("RESULT", session.pending["skip-old"], session.hostEpoch, "host-A", session.authorized))
        session.approve("host-B")
        assertTrue(session.authorized); assertTrue(session.pending.isEmpty())
    }
}

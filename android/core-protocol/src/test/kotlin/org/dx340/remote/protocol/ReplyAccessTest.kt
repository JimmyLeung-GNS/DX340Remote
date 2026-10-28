package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class ReplyAccessTest {
    @Test fun pendingApprovalAllowsProtocolAndErrorsButNoSourceData() {
        assertTrue(ReplyAccess.maySend("HELLO", true, false, false))
        assertTrue(ReplyAccess.maySend("ERROR", true, false, false))
        for (type in listOf("RESULT", "STATE_CHANGED", "ACCEPTED")) {
            assertFalse(ReplyAccess.maySend(type, true, false, false))
        }
    }
    @Test fun revocationOrLostSessionWithholdsLateBrowseAndStateReplies() {
        assertTrue(ReplyAccess.maySend("RESULT", true, true, true))
        assertFalse(ReplyAccess.maySend("RESULT", true, false, true))
        assertFalse(ReplyAccess.maySend("RESULT", true, true, false))
        assertFalse(ReplyAccess.maySend("STATE_CHANGED", true, false, false))
        assertFalse(ReplyAccess.maySend("STATE_CHANGED", false, true, true))
        assertFalse(ReplyAccess.maySend("HELLO", false, false, false))
    }
}

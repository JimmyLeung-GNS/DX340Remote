package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class ControlHandoffTest {
    @Test fun explicitHandoffInvalidatesOldWorkWithoutReusingAnyControllerLease() {
        val authorization = AuthorizationSession()
        val a = authorization.authorize("phone-A")
        val revisionA = authorization.ownershipRevision()
        assertSame(a, authorization.authorize("phone-A"))
        assertEquals(revisionA, authorization.ownershipRevision())
        val b = authorization.takeControl("phone-B", revisionA)
        assertFalse(authorization.isCurrent(a)); assertTrue(authorization.isCurrent(b))
        authorization.revoke("phone-A") // Late disconnect of the displaced transport.
        assertTrue(authorization.isCurrent(b))
        assertThrows(IllegalArgumentException::class.java) { authorization.takeControl("phone-C", revisionA) }
        assertTrue(authorization.isCurrent(b))
        val aAgain = authorization.takeControl("phone-A", authorization.ownershipRevision())
        assertNotSame(a, aAgain); assertFalse(authorization.isCurrent(a)); assertFalse(authorization.isCurrent(b))
    }
    @Test fun revokedOrDisconnectedOwnerMakesPendingTakeoverProposalStale() {
        val authorization = AuthorizationSession(); authorization.authorize("phone-A")
        val revision = authorization.ownershipRevision()
        authorization.revoke("phone-A")
        assertThrows(IllegalArgumentException::class.java) { authorization.takeControl("phone-B", revision) }
        val b = authorization.authorize("phone-B")
        assertTrue(authorization.isCurrent(b))
    }
    @Test fun proposalRequiresObservedHostAndCannotBeReplayedAfterSendFailureOrReconnect() {
        val proposal = ControlProposal()
        proposal.observeBusy("other-host", "host-A", "revision-1"); assertFalse(proposal.available)
        proposal.observeBusy("host-A", "host-A", "revision-1")
        assertEquals("revision-1", proposal.consume()); assertNull(proposal.consume())
        proposal.observeBusy("host-A", "host-A", "revision-2"); proposal.clear(); assertNull(proposal.consume())
        for (bad in listOf("", "unsafe:value", "字", "a".repeat(65))) {
            proposal.observeBusy("host-A", "host-A", bad); assertFalse(proposal.available)
        }
        proposal.observeBusy("", "", "revision-1"); assertFalse(proposal.available)
    }
    @Test fun secondTransportRequiresExistingApprovalAndBondEvenWhilePairingIsOpen() {
        assertTrue(PeerAdmission.allowed(0, false, false, true))
        assertFalse(PeerAdmission.allowed(0, false, true, false))
        assertTrue(PeerAdmission.allowed(0, true, true, false))
        assertTrue(PeerAdmission.allowed(1, true, true, false))
        assertFalse(PeerAdmission.allowed(1, false, true, true))
        assertFalse(PeerAdmission.allowed(1, true, false, true))
        assertFalse(PeerAdmission.allowed(2, true, true, true))
    }
    @Test fun controlLossNoticeIsApprovedOnlyAndDoesNotAuthorizeSourceReplies() {
        assertTrue(ReplyAccess.maySend("CONTROL_CHANGED", true, true, false))
        assertFalse(ReplyAccess.maySend("CONTROL_CHANGED", true, false, false))
        assertFalse(ReplyAccess.maySend("CONTROL_CHANGED", false, true, true))
        assertFalse(ReplyAccess.maySend("STATE_CHANGED", true, true, false))
        assertFalse(ReplyAccess.maySend("RESULT", true, true, false))
    }
}

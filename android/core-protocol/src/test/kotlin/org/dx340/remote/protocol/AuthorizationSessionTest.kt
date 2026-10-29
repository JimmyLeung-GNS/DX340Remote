package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class AuthorizationSessionTest {
    @Test fun repeatedAuthorizationOnTheSameConnectionDoesNotCancelItsOwnWork() {
        val session = AuthorizationSession(); val lease = session.authorize("phone-A")
        assertSame(lease, session.authorize("phone-A"))
        assertSame(lease, session.current("phone-A")); assertNull(session.current("phone-B"))
        assertTrue(session.isCurrent(lease)); assertFalse(session.isCurrent(null))
    }
    @Test fun revokingAnotherControllerPreservesTheActiveController() {
        val session = AuthorizationSession(); val lease = session.authorize("phone-A")
        session.revoke("phone-B")
        assertTrue(session.isCurrent(lease))
        assertThrows(IllegalStateException::class.java) { session.authorize("phone-B") }
    }
    @Test fun reconnectingTheSameAddressCannotReviveAnOldCallback() {
        val session = AuthorizationSession(); val before = session.authorize("phone-A")
        val delayedPermissionCheck = { session.isCurrent(before) }
        session.revoke("phone-A"); val after = session.authorize("phone-A")
        assertFalse(delayedPermissionCheck()); assertNotSame(before, after)
        assertTrue(session.isCurrent(after))
    }
    @Test fun revokeReapproveAndOtherControllerHandoffDoNotReuseLeases() {
        val session = AuthorizationSession(); val first = session.authorize("phone-A")
        session.revoke("phone-A"); val second = session.authorize("phone-B")
        session.revoke("phone-B"); val third = session.authorize("phone-A")
        assertFalse(session.isCurrent(first)); assertFalse(session.isCurrent(second)); assertTrue(session.isCurrent(third))
        session.clear(); assertFalse(session.isCurrent(third))
    }
    @Test fun oldHostInstancesCannotAuthorizeEachOthersLeases() {
        val old = AuthorizationSession().authorize("phone-A")
        val fresh = AuthorizationSession(); fresh.authorize("phone-A")
        assertFalse(fresh.isCurrent(old))
    }
}

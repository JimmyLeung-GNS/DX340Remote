package org.dx340.remote.sources

import org.dx340.remote.protocol.AuthorizationSession
import org.dx340.remote.protocol.MutationGate
import org.junit.Assert.*
import org.junit.Test

class ControllerHandoffCancellationTest {
    @Test fun oldNativeCallbackCannotPublishOrDispatchAfterAnApprovedControllerTakesOver() {
        val authorization = AuthorizationSession(); val old = authorization.authorize("phone-A")
        val gate = MutationGate(); val oldMutation = gate.enter("same-id")
        var dispatches = 0; var error: String? = null
        val request = SourceRequest<String>({ if (authorization.isCurrent(old)) null else "AUTHORIZATION_DENIED" }, { gate.leave(oldMutation) }) { _, reason -> error = reason }
        authorization.takeControl("phone-B", authorization.ownershipRevision())
        if (request.check()) { dispatches++; request.finish("stale source result", null) }
        assertEquals(0, dispatches); assertEquals("AUTHORIZATION_DENIED", error)
        val newMutation = gate.enter("same-id")
        request.finish("late duplicate", null); gate.leave(oldMutation)
        assertThrows(IllegalStateException::class.java) { gate.enter("third") }
        gate.leave(newMutation); gate.enter("third")
    }
}

package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class InputWriteReplyTest {
    private val payload = byteArrayOf(1, 2, 3)
    @Test fun completeValidCommandSubmitsResponseBeforeOneDispatch() {
        val events = mutableListOf<String>()
        InputWriteReply.deliver(true, true, payload, { events += "response"; true },
            { events += "failure" }, { assertSame(payload, it); events += "dispatch" })
        assertEquals(listOf("response", "dispatch"), events)
    }
    @Test fun failedSubmissionNeverInvokesCommandEvenWithACompleteValidPayload() {
        val events = mutableListOf<String>()
        InputWriteReply.deliver(true, true, payload, { events += "response"; false },
            { events += "failure" }, { events += "dispatch" })
        assertEquals(listOf("response", "failure"), events)
    }
    @Test fun responseExceptionAlsoFailsWithoutDispatch() {
        var failures = 0; var mutations = 0
        InputWriteReply.deliver(true, true, payload, { throw SecurityException("permission lost") },
            { failures++ }, { mutations++ })
        assertEquals(1, failures); assertEquals(0, mutations)
    }
    @Test fun rejectedInputCanReceiveErrorResponseButCannotDispatch() {
        var responses = 0; var failures = 0; var mutations = 0
        InputWriteReply.deliver(true, false, payload, { responses++; true }, { failures++ }, { mutations++ })
        assertEquals(1, responses); assertEquals(0, failures); assertEquals(0, mutations)
    }
    @Test fun incompleteFrameIsAcknowledgedWithoutDispatchingOrStagingACommand() {
        var responses = 0; var mutations = 0
        InputWriteReply.deliver(true, true, null, { responses++; true }, { fail("unexpected failure") }, { mutations++ })
        assertEquals(1, responses); assertEquals(0, mutations)
    }
    @Test fun unsupportedWriteWithoutResponseCannotDispatchOrSubmitAnUnrequestedReply() {
        var calls = 0
        InputWriteReply.deliver(false, true, payload, { calls++; true }, { calls++ }, { calls++ })
        assertEquals(0, calls)
    }
    @Test fun sourceDispatchExceptionIsNotMisreportedAsResponseSubmissionFailure() {
        var failures = 0
        try {
            InputWriteReply.deliver(true, true, payload, { true }, { failures++ }, { throw IllegalStateException("source error") })
            fail("source error must retain its own handling")
        } catch (error: IllegalStateException) { assertEquals("source error", error.message) }
        assertEquals(0, failures)
    }
}

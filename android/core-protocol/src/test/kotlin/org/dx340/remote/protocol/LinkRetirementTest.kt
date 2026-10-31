package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class LinkRetirementTest {
    private class Timer {
        val tasks = mutableListOf<() -> Unit>()
        val cancelled = mutableSetOf<Int>()
        fun schedule(delay: Long, action: () -> Unit): () -> Unit {
            assertEquals(2000L, delay)
            val index = tasks.size; tasks += action
            return { cancelled += index }
        }
    }
    @Test fun callbackClosesBeforeReplacementAndCancelsDeadline() {
        val timer = Timer(); val state = LinkRetirement<Any>(timer::schedule); val link = Any(); val events = mutableListOf<String>()
        state.retire(link, { events += "disconnect" }, { events += "close:$it" })
        state.whenIdle { events += "connect" }
        assertEquals(listOf("disconnect"), events)
        assertTrue(state.disconnected(link))
        assertEquals(listOf("disconnect", "close:disconnected", "connect"), events)
        assertTrue(0 in timer.cancelled)
        timer.tasks[0](); assertEquals(3, events.size)
    }
    @Test fun missingCallbackIsBoundedAndLateOldCallbackCannotCloseReplacement() {
        val timer = Timer(); val state = LinkRetirement<Any>(timer::schedule); val old = Any(); val next = Any(); val closed = mutableListOf<String>()
        state.retire(old, {}, { closed += "old:$it" })
        state.whenIdle { state.retire(next, {}, { closed += "next:$it" }) }
        timer.tasks[0]()
        assertEquals(listOf("old:deadline"), closed)
        assertFalse(state.disconnected(old)); timer.tasks[0]()
        assertTrue(state.contains(next)); assertEquals(1, closed.size)
        state.disconnected(next); assertEquals(listOf("old:deadline", "next:disconnected"), closed)
    }
    @Test fun userCancelPreventsQueuedReconnectButStillReleasesOldClient() {
        val timer = Timer(); val state = LinkRetirement<Any>(timer::schedule); val link = Any(); val events = mutableListOf<String>()
        state.retire(link, {}, { events += "closed" }); state.whenIdle { events += "connect" }
        state.cancelWaiting(); timer.tasks[0]()
        assertEquals(listOf("closed"), events)
    }
    @Test fun onlyLatestExplicitConnectionChoiceStartsAfterCleanup() {
        val timer = Timer(); val state = LinkRetirement<Any>(timer::schedule); val link = Any(); val events = mutableListOf<String>()
        state.retire(link, {}, {}); state.whenIdle { events += "obsolete" }; state.whenIdle { events += "latest" }
        state.disconnected(link); assertEquals(listOf("latest"), events)
    }
    @Test fun disconnectFailureReleasesImmediatelyAndCallbackIdentityIsExact() {
        val timer = Timer(); val state = LinkRetirement<Any>(timer::schedule); val events = mutableListOf<String>()
        val first = String(charArrayOf('x')); val equalButDifferent = String(charArrayOf('x'))
        state.retire(first, {}, { events += it }); assertFalse(state.disconnected(equalButDifferent)); state.disconnected(first)
        state.retire(Any(), { throw IllegalStateException("radio unavailable") }, { events += it })
        state.whenIdle { events += "idle" }
        assertEquals(listOf("disconnected", "disconnect_failed", "idle"), events)
    }
}

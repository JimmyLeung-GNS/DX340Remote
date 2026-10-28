package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class PlaybackHandoffTest {
    private class Rig {
        var clock = 0L
        val tasks = ArrayDeque<() -> Unit>()
        var other = HandoffSource("mango", "session1", HandoffPlayback.ACTIVE, true)
        var extra: HandoffSource? = null
        var guardError: String? = null
        var pauses = 0
        val outcomes = mutableListOf<String?>()
        fun start() = PlaybackHandoff({ clock }, { _, task -> tasks.add(task) }, { listOfNotNull(other, extra) }, { pauses++ })
            .prepare("apple", { guardError }, { outcomes.add(it) })
        fun tick() { clock += 100; if (tasks.isNotEmpty()) tasks.removeFirst().invoke() }
    }
    @Test fun waitsForObservedPauseAndFinishesOnceWithoutRetry() {
        val r = Rig(); r.start(); repeat(3) { r.tick() }
        assertEquals(1, r.pauses); assertTrue(r.outcomes.isEmpty())
        r.other = r.other.copy(playback = HandoffPlayback.PAUSED); r.tick(); r.tick()
        assertEquals(listOf<String?>(null), r.outcomes); assertEquals(1, r.pauses)
    }
    @Test fun idleSourceRequiresNoPause() {
        for (state in listOf(HandoffPlayback.PAUSED, HandoffPlayback.STOPPED)) {
            val r = Rig(); r.other = r.other.copy(playback = state); r.start()
            assertEquals(0, r.pauses); assertEquals(listOf<String?>(null), r.outcomes)
        }
    }
    @Test fun unsupportedPauseDoesNotDispatch() {
        val r = Rig(); r.other = r.other.copy(canPause = false); r.start()
        assertEquals(listOf("OTHER_SOURCE_PLAYING"), r.outcomes); assertEquals(0, r.pauses)
    }
    @Test fun timeoutHasNoAutomaticRetryOrSuccess() {
        val r = Rig(); r.start(); repeat(40) { r.tick() }
        assertEquals(listOf("SOURCE_HANDOFF_FAILED"), r.outcomes); assertEquals(1, r.pauses)
    }
    @Test fun revokedBeforeRequestDoesNotPause() {
        val r = Rig(); r.guardError = "AUTHORIZATION_DENIED"; r.start()
        assertEquals(0, r.pauses); assertEquals(listOf("AUTHORIZATION_DENIED"), r.outcomes)
    }
    @Test fun revokedOrChangedTargetDuringWaitCannotStartTarget() {
        for (error in listOf("AUTHORIZATION_DENIED", "STALE_SOURCE", "PERMISSION_REVOKED")) {
            val r = Rig(); r.start(); r.guardError = error
            r.other = r.other.copy(playback = HandoffPlayback.PAUSED); r.tick()
            assertEquals(listOf(error), r.outcomes); assertEquals(1, r.pauses)
        }
    }
    @Test fun replacedSourceSessionCannotSatisfyOldPause() {
        val r = Rig(); r.start(); r.other = r.other.copy(epoch = "session2", playback = HandoffPlayback.PAUSED); r.tick()
        assertEquals(listOf("SOURCE_HANDOFF_FAILED"), r.outcomes)
    }
    @Test fun unknownOrMultipleActiveSourcesFailClosed() {
        val unknown = Rig(); unknown.other = unknown.other.copy(playback = HandoffPlayback.UNKNOWN); unknown.start()
        assertEquals(listOf("SOURCE_HANDOFF_FAILED"), unknown.outcomes); assertEquals(0, unknown.pauses)
        val multiple = Rig(); multiple.extra = multiple.other.copy(id = "third"); multiple.start()
        assertEquals(listOf("OTHER_SOURCE_PLAYING"), multiple.outcomes); assertEquals(0, multiple.pauses)
    }
    @Test fun anotherSourceStartingDuringWaitPreventsSuccess() {
        val r = Rig(); r.start(); r.other = r.other.copy(playback = HandoffPlayback.PAUSED)
        r.extra = r.other.copy(id = "third", playback = HandoffPlayback.ACTIVE); r.tick()
        assertEquals(listOf("SOURCE_HANDOFF_FAILED"), r.outcomes)
    }
}

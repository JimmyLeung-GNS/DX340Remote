package org.dx340.remote.sources

import org.dx340.remote.protocol.CommandLedger
import org.dx340.remote.protocol.AuthorizationSession
import org.junit.Assert.*
import org.junit.Test

class OutputVolumeTest {
    private class Rig {
        var sample = VolumeSample("mango-session-output", 35, 0, 100, true)
        var acceptWrite = true
        val writes = mutableListOf<Int>()
        val callbacks = ArrayDeque<() -> Unit>()
        val volume = OutputVolume({ sample }, { writes += it; if (acceptWrite) sample = sample.copy(current = it) }, { _, task -> callbacks += task }, {})
        fun settle() { callbacks.removeFirst().invoke() }
        fun verified(): VolumeProof {
            volume.begin("AMP15 phone out", 35) { assertNull(it) }; settle()
            val id = volume.check!!.id
            volume.confirmDown(id); volume.returnStep(id) { assertNull(it) }; settle(); volume.confirmReturn(id)
            return volume.proof(sample)!!
        }
    }
    @Test fun onlyTwoHumanConfirmationsEnableExactBand() {
        val r = Rig(); r.volume.begin("AMP15 phone out", 35) { assertNull(it) }; r.settle()
        assertNull(r.volume.proof(r.sample)); val id = r.volume.check!!.id
        r.volume.confirmDown(id); assertNull(r.volume.proof(r.sample))
        r.volume.returnStep(id) { assertNull(it) }; r.settle(); assertNull(r.volume.proof(r.sample))
        r.volume.confirmReturn(id); assertEquals(34, r.volume.proof(r.sample)!!.low); assertEquals(35, r.volume.proof(r.sample)!!.high)
        assertEquals(listOf(34, 35), r.writes)
    }
    @Test fun wheelChangeAndMalformedInputsNeverWrite() {
        val r = Rig(); val p = r.verified(); r.writes.clear(); r.sample = r.sample.copy(current = 34)
        var error: String? = null
        r.volume.adjust(-1, 35, p.id, { true }) { error = it }; assertEquals("STALE_VOLUME", error)
        for (delta in listOf(0, 2, "1", 1.0, true)) r.volume.adjust(delta, 34, p.id, { true }) { assertEquals("INVALID_VOLUME_STEP", it) }
        r.volume.adjust(1, "34", p.id, { true }) { assertEquals("INVALID_VOLUME_STEP", it) }
        assertTrue(r.writes.isEmpty())
    }
    @Test fun authorizationSourceOutputAndBoundsAreRechecked() {
        val r = Rig(); val p = r.verified(); r.writes.clear()
        r.volume.adjust(-1, 35, p.id, { false }) { assertEquals("AUTHORIZATION_DENIED", it) }
        r.volume.adjust(1, 35, p.id, { true }) { assertEquals("VOLUME_AT_BOUNDARY", it) }
        r.volume.adjust(-1, 35, "old-profile", { true }) { assertEquals("VOLUME_PROFILE_CHANGED", it) }
        r.sample = r.sample.copy(key = "apple-session-output")
        r.volume.adjust(-1, 35, p.id, { true }) { assertEquals("VOLUME_VERIFICATION_REQUIRED", it) }
        assertTrue(r.writes.isEmpty())
    }
    @Test fun changedContextBetweenStepAndConfirmationCannotVerify() {
        val r = Rig(); r.volume.begin("AMP15", 35) {}; r.settle()
        val id = r.volume.check!!.id; r.sample = r.sample.copy(key = "new-output")
        try { r.volume.confirmDown(id); fail() } catch (e: IllegalArgumentException) { assertEquals("VOLUME_PROFILE_CHANGED", e.message) }
        assertNull(r.volume.proof(r.sample)); assertEquals(listOf(34), r.writes)
    }
    @Test fun delayedObservationFailureRevokesAndDoesNotCompensate() {
        val r = Rig(); val p = r.verified(); r.writes.clear(); r.acceptWrite = false
        r.volume.adjust(-1, 35, p.id, { true }) { assertEquals("STALE_VOLUME", it) }; r.settle()
        assertNull(r.volume.proof(r.sample)); assertEquals(listOf(34), r.writes)
    }
    @Test fun concurrentLocalAndRemoteRequestsStaySerialized() {
        val r = Rig(); val p = r.verified(); r.writes.clear()
        r.volume.adjust(-1, 35, p.id, { true }) { assertNull(it) }
        r.volume.adjust(1, 34, p.id, { true }) { assertEquals("SOURCE_BUSY", it) }
        assertTrue(r.volume.busy); r.settle(); assertFalse(r.volume.busy); assertEquals(listOf(34), r.writes)
    }
    @Test fun duplicateAndNewRequestWithOldValueNeverStepTwice() {
        val r = Rig(); val p = r.verified(); r.writes.clear(); val ledger = CommandLedger()
        assertNull(ledger.begin("command", "payload"))
        r.volume.adjust(-1, 35, p.id, { true }) { ledger.finish("command", "observed34") }
        assertEquals("PENDING", ledger.begin("command", "payload")); r.settle()
        assertEquals("observed34", ledger.begin("command", "payload"))
        r.volume.adjust(-1, 35, p.id, { true }) { assertEquals("STALE_VOLUME", it) }
        assertEquals(listOf(34), r.writes)
    }
    @Test fun resetStopOrPlaybackChangeCannotReuseProof() {
        val r = Rig(); val p = r.verified(); r.writes.clear(); r.volume.reset()
        r.volume.adjust(-1, 35, p.id, { true }) { assertEquals("VOLUME_VERIFICATION_REQUIRED", it) }
        val s = Rig(); s.verified(); s.sample = s.sample.copy(ready = false); assertNull(s.volume.proof(s.sample))
        s.volume.close(); assertNull(s.volume.proof(s.sample)); assertTrue(r.writes.isEmpty())
    }
    @Test fun revokedAuthorizationAfterWriteNeverTriggersRestorationOrRetry() {
        val r = Rig(); val p = r.verified(); r.writes.clear(); var authorized = true
        r.volume.adjust(-1, 35, p.id, { authorized }) { assertEquals("AUTHORIZATION_DENIED", it) }
        authorized = false; r.settle()
        assertEquals(34, r.sample.current); assertEquals(listOf(34), r.writes); assertNull(r.volume.proof(r.sample))
    }
    @Test fun outputChangeDuringReadbackInvalidatesTheListeningCheck() {
        val r = Rig(); r.volume.begin("AMP15 phone out", 35) { assertEquals("VOLUME_PROFILE_CHANGED", it) }
        r.sample = r.sample.copy(key = "different-output"); r.settle()
        assertNull(r.volume.check); assertNull(r.volume.proof(r.sample)); assertEquals(listOf(34), r.writes)
    }
    @Test fun sameControllerReauthorizationCannotReviveOldVolumeWork() {
        val r = Rig(); val p = r.verified(); r.writes.clear()
        val authorization = AuthorizationSession(); val old = authorization.authorize("pc")
        r.volume.adjust(-1, 35, p.id, { authorization.isCurrent(old) }) { assertEquals("AUTHORIZATION_DENIED", it) }
        authorization.revoke("pc"); val replacement = authorization.authorize("pc")
        assertTrue(authorization.isCurrent(replacement)); assertFalse(authorization.isCurrent(old))
        r.settle(); assertNull(r.volume.proof(r.sample)); assertEquals(listOf(34), r.writes)
        r.volume.adjust(1, 34, p.id, { authorization.isCurrent(old) }) { assertEquals("VOLUME_VERIFICATION_REQUIRED", it) }
        assertEquals(listOf(34), r.writes)
    }
    @Test fun hardwareWheelOutsideConfirmedBandCannotBeUsedAsNewRemoteBaseline() {
        val r = Rig(); val p = r.verified(); r.writes.clear()
        for (level in listOf(33, 36)) {
            r.sample = r.sample.copy(current = level)
            assertNull(r.volume.proof(r.sample))
            r.volume.adjust(if (level == 33) 1 else -1, level, p.id, { true }) { assertEquals("VOLUME_VERIFICATION_REQUIRED", it) }
        }
        assertTrue(r.writes.isEmpty())
    }
    @Test fun newListeningCheckCannotTemporarilyExposeAnOlderBand() {
        val r = Rig(); r.verified(); r.sample = r.sample.copy(current = 36)
        r.volume.begin("AMP15 phone out", 36) { assertNull(it) }; r.settle()
        assertEquals(35, r.sample.current) // Back inside the old34–35 band, but not confirmed yet.
        assertNull(r.volume.proof(r.sample))
        val id = r.volume.check!!.id; r.volume.confirmDown(id)
        r.volume.returnStep(id) { assertNull(it) }; r.settle(); assertNull(r.volume.proof(r.sample))
        r.volume.confirmReturn(id); assertEquals(36, r.volume.proof(r.sample)!!.high)
    }
}

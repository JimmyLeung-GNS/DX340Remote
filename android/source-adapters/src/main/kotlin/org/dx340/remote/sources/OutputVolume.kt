package org.dx340.remote.sources

import java.util.UUID

/** A fresh public-API observation. key binds source session, app/build and output inventory. */
data class VolumeSample(val key: String, val current: Int, val minimum: Int, val maximum: Int, val ready: Boolean)
data class VolumeProof(val key: String, val id: String, val label: String, val low: Int, val high: Int)
data class VolumeCheck(val id: String, val sample: VolumeSample, val label: String, val phase: String)

/** Main-thread coordinator shared by local checks and BLE commands. No automatic write retries. */
class OutputVolume(private val read: () -> VolumeSample, private val write: (Int) -> Unit,
    private val later: (Long, () -> Unit) -> Unit, private val changed: () -> Unit) {
    private val proofs = mutableMapOf<String, VolumeProof>()
    var check: VolumeCheck? = null; private set
    var busy = false; private set
    private var closed = false
    fun sample() = read()
    fun proof(sample: VolumeSample): VolumeProof? = proofs[sample.key]?.takeIf { check == null && sample.ready && sample.current in it.low..it.high }
    fun reset() { proofs.clear(); check = null; changed() }
    fun close() { closed = true; proofs.clear(); check = null }
    private fun fresh(expected: VolumeSample, allowed: () -> Boolean): VolumeSample {
        require(!closed) { "SOURCE_NOT_READY" }
        require(allowed()) { "AUTHORIZATION_DENIED" }
        val now = read()
        require(now.key == expected.key) { "VOLUME_PROFILE_CHANGED" }
        require(now.ready) { "VOLUME_SOURCE_NOT_READY" }
        require(now.current == expected.current) { "STALE_VOLUME" }
        return now
    }
    private fun step(expected: VolumeSample, target: Int, allowed: () -> Boolean, done: (String?) -> Unit) {
        var entered = false
        try {
            require(!busy) { "SOURCE_BUSY" }
            val now = fresh(expected, allowed)
            require(target in now.minimum..now.maximum && kotlin.math.abs(target.toLong() - now.current) == 1L) { "INVALID_VOLUME_STEP" }
            busy = true; entered = true
            write(target)
            later(500) {
                val error = try {
                    val observed = fresh(expected.copy(current = target), allowed)
                    require(observed.minimum == expected.minimum && observed.maximum == expected.maximum) { "VOLUME_PROFILE_CHANGED" }
                    null
                } catch (e: Exception) { e.message ?: "VOLUME_NOT_OBSERVED" }
                busy = false
                // Unexpected hardware observations revoke every band. Never compensate with another write.
                if (error != null) { proofs.clear(); check = null }
                changed(); done(error)
            }
        } catch (e: Exception) { if (entered) { busy = false; proofs.clear(); check = null }; done(e.message ?: "VOLUME_NOT_OBSERVED") }
    }
    fun begin(label: String, expected: Int, done: (String?) -> Unit) {
        try {
            require(!busy && check == null) { "SOURCE_BUSY" }
            require(label.isNotBlank() && label.length <= 80) { "OUTPUT_LABEL_REQUIRED" }
            val now = read(); require(now.current == expected) { "STALE_VOLUME" }
            require(now.current > now.minimum) { "VOLUME_AT_BOUNDARY" }
            val pending = VolumeCheck(UUID.randomUUID().toString(), now, label.trim(), "down")
            check = pending
            step(now, now.current - 1, { check?.id == pending.id }) { error ->
                check = if (error == null) pending else null
                changed(); done(error)
            }
        } catch (e: Exception) { done(e.message ?: "VOLUME_NOT_OBSERVED") }
    }
    fun confirmDown(id: String) {
        require(!busy) { "SOURCE_BUSY" }
        val pending = check?.takeIf { it.id == id && it.phase == "down" } ?: error("EXPIRED_VOLUME_CHECK")
        fresh(pending.sample.copy(current = pending.sample.current - 1), { true })
        check = pending.copy(phase = "return"); changed()
    }
    fun returnStep(id: String, done: (String?) -> Unit) {
        val pending = check?.takeIf { it.id == id && it.phase == "return" } ?: return done("EXPIRED_VOLUME_CHECK")
        step(pending.sample.copy(current = pending.sample.current - 1), pending.sample.current, { check?.id == id }) { error ->
            check = if (error == null) pending.copy(phase = "up") else null
            changed(); done(error)
        }
    }
    fun confirmReturn(id: String) {
        require(!busy) { "SOURCE_BUSY" }
        val pending = check?.takeIf { it.id == id && it.phase == "up" } ?: error("EXPIRED_VOLUME_CHECK")
        fresh(pending.sample, { true })
        if (proofs.size >= 8 && pending.sample.key !in proofs) proofs.remove(proofs.keys.first())
        proofs[pending.sample.key] = VolumeProof(pending.sample.key, UUID.randomUUID().toString(), pending.label, pending.sample.current - 1, pending.sample.current)
        check = null; changed()
    }
    fun adjust(delta: Any?, expected: Any?, profileId: String, allowed: () -> Boolean, done: (String?) -> Unit) {
        try {
            require(delta is Int && delta in listOf(-1, 1) && expected is Int) { "INVALID_VOLUME_STEP" }
            require(check == null && !busy) { "SOURCE_BUSY" }
            val now = read(); val proof = proof(now) ?: error("VOLUME_VERIFICATION_REQUIRED")
            require(proof.id == profileId) { "VOLUME_PROFILE_CHANGED" }
            require(now.current == expected) { "STALE_VOLUME" }
            require(now.current + delta in proof.low..proof.high) { "VOLUME_AT_BOUNDARY" }
            step(now, now.current + delta, allowed, done)
        } catch (e: Exception) { done(e.message ?: "VOLUME_NOT_OBSERVED") }
    }
}

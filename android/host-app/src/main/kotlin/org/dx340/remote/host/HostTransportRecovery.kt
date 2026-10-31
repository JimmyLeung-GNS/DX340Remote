package org.dx340.remote.host

/** Bounded transport recreation, never command retries or controller approval. */
internal class HostTransportRecovery {
    private val attempts = ArrayDeque<Long>()
    private var lastClock: Long? = null
    fun request(now: Long, enabled: Boolean, permissions: Boolean, radioOn: Boolean): Boolean {
        if (!enabled || !permissions || !radioOn || now < 0 || lastClock?.let { now < it } == true) return false
        lastClock = now
        while (attempts.firstOrNull()?.let { now - it >= 60000 } == true) attempts.removeFirst()
        if (attempts.size >= 2) return false
        attempts.addLast(now)
        return true
    }
    fun explicitRestart() { attempts.clear(); lastClock = null }
}

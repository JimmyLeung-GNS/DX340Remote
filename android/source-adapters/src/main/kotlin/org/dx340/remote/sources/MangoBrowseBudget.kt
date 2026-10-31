package org.dx340.remote.sources

/** Bounded source navigation time; no request, authorization or playback retry. */
class MangoBrowseBudget(private val startedAt: Long) {
    init { require(startedAt >= 0) }
    private var sdReload = false
    fun remainingMs(now: Long): Long {
        if (now < startedAt) return -1
        val limit = if (sdReload) 10000L else 8000L
        val elapsed = now - startedAt
        return if (elapsed > limit) -1 else limit - elapsed
    }
    fun expired(now: Long) = remainingMs(now) < 0
    fun beginSdReload(now: Long) {
        check(!expired(now)) { "UI_ACTION_TIMEOUT" }
        // Anchored to the original start, so repeated observations cannot renew it.
        sdReload = true
    }
}

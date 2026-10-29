package org.dx340.remote.sources

/** One launch may wait for its first window; later foreground loss must fail closed. */
class UiForegroundStartup(private val waitMs: Long = 2000) {
    enum class Missing { LAUNCH, WAIT, FAIL }
    private var launchedAt: Long? = null
    private var observed = false
    fun observed() { observed = true }
    fun missing(now: Long): Missing {
        if (observed) return Missing.FAIL
        val started = launchedAt
        if (started == null) { launchedAt = now; return Missing.LAUNCH }
        return if (now >= started && now - started < waitMs) Missing.WAIT else Missing.FAIL
    }
}

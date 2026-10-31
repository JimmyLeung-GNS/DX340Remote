package org.dx340.remote.host

/** Main-thread, volatile, one-shot diagnostic. No persisted arm or BLE operation. */
internal class HostTransportFaults(private val clock: () -> Long) {
    private var token: Any? = null
    private var armedAt = 0L
    private var lastClock = 0L

    private fun active(now: Long): Boolean {
        if (now < 0 || now < lastClock || (token != null && now - armedAt >= 20000L)) {
            clear(); return false
        }
        lastClock = now
        return token != null
    }
    fun arm(link: Any): Boolean {
        val now = clock()
        // Clock regression/invalid time fails this attempt, rather than rearming.
        if (now < 0 || now < lastClock) { clear(); return false }
        if (active(now)) return false
        token = link; armedAt = now; lastClock = now
        return true
    }
    fun ignoreCompletion(link: Any, success: Boolean): Boolean {
        if (!active(clock()) || token !== link || !success) return false
        clear(); return true
    }
    fun clear() { token = null; armedAt = 0L; lastClock = 0L }
}

package org.dx340.remote.host

/** Main-thread volatile diagnostics. No persisted arm or staged-message dispatch. */
internal class HostTransportFaults(private val clock: () -> Long,
    private val schedule: ((Long, () -> Unit) -> (() -> Unit))? = null) {
    private enum class Mode { CALLBACK, WRITE }
    private var token: Any? = null
    private var mode = Mode.CALLBACK
    private var armedAt = 0L
    private var lastClock = 0L
    private var held: Any? = null
    private var cancelHold: (() -> Unit)? = null

    private fun active(now: Long): Boolean {
        if (now < 0 || now < lastClock || (token != null && now - armedAt >= 20000L)) {
            clear(); return false
        }
        lastClock = now
        return token != null
    }
    fun arm(link: Any): Boolean = arm(link, Mode.CALLBACK)
    fun armWrite(link: Any): Boolean = schedule != null && arm(link, Mode.WRITE)
    private fun arm(link: Any, selected: Mode): Boolean {
        if (held != null) return false
        val now = clock()
        // Clock regression/invalid time fails this attempt, rather than rearming.
        if (now < 0 || now < lastClock) { clear(); return false }
        if (active(now)) return false
        token = link; mode = selected; armedAt = now; lastClock = now
        return true
    }
    fun ignoreCompletion(link: Any, success: Boolean): Boolean {
        if (mode != Mode.CALLBACK || !active(clock()) || token !== link || !success) return false
        clear(); return true
    }
    fun withholdWriteResponse(link: Any, expire: () -> Unit): Boolean {
        val scheduler = schedule ?: return false
        if (mode != Mode.WRITE || !active(clock()) || token !== link) return false
        clear()
        val generation = Any(); held = generation
        val cancel = scheduler(5000L) {
            if (held === generation) {
                held = null; cancelHold = null
                expire()
            }
        }
        if (held === generation) cancelHold = cancel else cancel()
        return true
    }
    fun clear() {
        token = null; armedAt = 0L; lastClock = 0L
        held = null
        val cancel = cancelHold; cancelHold = null; cancel?.invoke()
    }
}

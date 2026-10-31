package org.dx340.remote.protocol

/** Main-thread transport cleanup. Release an old client before starting its replacement. */
class LinkRetirement<T : Any>(private val schedule: (Long, () -> Unit) -> (() -> Unit)) {
    companion object { const val TIMEOUT_MS = 2000L }
    private class Entry<T>(val token: T, val close: (String) -> Unit) { var cancel: (() -> Unit)? = null }
    private var retiring: Entry<T>? = null
    private var waiting: (() -> Unit)? = null

    fun contains(token: T) = retiring?.token === token
    fun whenIdle(action: () -> Unit) {
        waiting = action
        drain()
    }
    fun cancelWaiting() { waiting = null }
    fun retire(token: T, disconnect: () -> Unit, close: (String) -> Unit) {
        check(retiring == null) { "Previous link must retire before replacement" }
        val entry = Entry(token, close); retiring = entry
        val cancel = schedule(TIMEOUT_MS) { finish(entry, "deadline") }
        if (retiring === entry) entry.cancel = cancel else cancel()
        if (retiring === entry) {
            try { disconnect() } catch (_: Exception) { finish(entry, "disconnect_failed") }
        }
    }
    fun disconnected(token: T): Boolean {
        val entry = retiring?.takeIf { it.token === token } ?: return false
        finish(entry, "disconnected")
        return true
    }
    private fun finish(entry: Entry<T>, reason: String) {
        if (retiring !== entry) return
        // Invalidate first: cancellation or a late callback cannot retire a replacement.
        retiring = null
        entry.cancel?.invoke(); entry.cancel = null
        try { entry.close(reason) } finally { drain() }
    }
    private fun drain() {
        if (retiring != null) return
        val action = waiting; waiting = null
        action?.invoke()
    }
}

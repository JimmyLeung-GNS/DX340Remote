package org.dx340.remote.sources

/** Permission/observer lifetime, separate from Android session objects for failure testing. */
class SourceSessionAccess(
    private val register: () -> Unit,
    private val unregister: () -> Unit,
    private val lost: () -> Unit
) {
    var error: String? = null; private set
    private var registered = false
    private var closed = false

    fun <T> refresh(read: () -> T): T? {
        if (closed) return null
        return try {
            val result = read()
            if (!registered) { register(); registered = true }
            error = null
            result
        } catch (_: SecurityException) {
            permissionDenied()
            null
        }
    }

    fun permissionDenied() {
        if (closed) return
        val newlyLost = error == null
        error = "NOTIFICATION_ACCESS_REQUIRED"
        stopObserving()
        if (newlyLost) lost()
    }

    private fun stopObserving() {
        val remove = registered
        registered = false
        if (remove) runCatching(unregister)
    }

    fun close() { closed = true; stopObserving() }
}

package org.dx340.remote.sources

/** Main-thread request lifetime. Late source callbacks must not mutate shared caches. */
class SourceRequest<T>(
    private val invalid: () -> String?,
    private val cleanup: () -> Unit,
    private val done: (T?, String?) -> Unit
) {
    private var completed = false

    /** Call before inspecting a callback or changing any source/cache state. */
    fun check(): Boolean {
        if (completed) return false
        val error = invalid()
        if (error != null) complete(null, error)
        return error == null
    }

    fun finish(value: T?, error: String?) {
        if (completed) return
        val rejected = invalid()
        complete(if (rejected == null) value else null, rejected ?: error)
    }

    fun cancel(error: String) {
        if (!completed) complete(null, error)
    }

    private fun complete(value: T?, error: String?) {
        // Mark first: unsubscribe and done can themselves trigger callbacks.
        completed = true
        try { cleanup() } finally { done(value, error) }
    }
}

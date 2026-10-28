package org.dx340.remote.sources

/** Main-thread ownership of callbacks; library revisions do not replace media-session ownership. */
class SourceCallbackGuard {
    class Lease internal constructor(internal val source: String)
    private val owners = mutableMapOf<String, Lease>()
    private var closed = false

    fun replace(source: String): Lease {
        check(!closed) { "SOURCE_REGISTRY_CLOSED" }
        return Lease(source).also { owners[source] = it }
    }
    fun dispatch(lease: Lease, action: () -> Unit) {
        if (!closed && owners[lease.source] === lease) action()
    }
    fun retire(lease: Lease): Boolean {
        if (closed || owners[lease.source] !== lease) return false
        owners.remove(lease.source)
        return true
    }
    fun invalidateAll() { owners.clear() }
    fun close() { closed = true; invalidateAll() }
}

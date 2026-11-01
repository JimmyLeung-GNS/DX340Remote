package org.dx340.remote.sources

/** Main-thread ownership of callbacks; library revisions do not replace media-session ownership. */
class SourceCallbackGuard {
    class Lease internal constructor(internal val source: String, internal var queueAvailable: Boolean)
    private val owners = mutableMapOf<String, Lease>()
    private var closed = false

    fun replace(source: String, queueAvailable: Boolean = false): Lease {
        check(!closed) { "SOURCE_REGISTRY_CLOSED" }
        return Lease(source, queueAvailable).also { owners[source] = it }
    }
    fun dispatch(lease: Lease, action: () -> Unit) {
        if (!closed && owners[lease.source] === lease) action()
    }
    /** Availability only: queue contents/revisions are still read fresh on explicit requests. */
    fun canReadQueue(source: String): Boolean = !closed && owners[source]?.queueAvailable == true
    /** An explicit refresh can recover a source that omitted its queue callback. */
    fun refreshQueue(source: String, read: () -> Boolean) {
        owners[source]?.let { lease -> dispatch(lease) { lease.queueAvailable = read() } }
    }
    fun queueChanged(lease: Lease, available: Boolean, action: () -> Unit) = dispatch(lease) {
        lease.queueAvailable = available
        action()
    }
    fun retire(lease: Lease): Boolean {
        if (closed || owners[lease.source] !== lease) return false
        owners.remove(lease.source)
        return true
    }
    fun invalidateAll() { owners.clear() }
    fun close() { closed = true; invalidateAll() }
}

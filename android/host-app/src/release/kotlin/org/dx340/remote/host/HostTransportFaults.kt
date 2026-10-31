package org.dx340.remote.host

/** Release contract: fault injection is unavailable, even if a caller requests it. */
@Suppress("UNUSED_PARAMETER")
internal class HostTransportFaults(clock: () -> Long) {
    fun arm(link: Any) = false
    fun ignoreCompletion(link: Any, success: Boolean) = false
    fun clear() = Unit
}

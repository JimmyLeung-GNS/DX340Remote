package org.dx340.remote.protocol

data class PlayerChoice(val target: String?, val choose: Boolean)

/** Only IDs supplied by the native, service-filtered scan participate. Names are not identities. */
object PlayerDiscovery {
    fun finish(found: Collection<String>, remembered: String?): PlayerChoice {
        val ids = found.filter { it.isNotEmpty() }.distinct()
        val saved = remembered?.takeIf { it.isNotEmpty() }
        if (saved != null && saved in ids) return PlayerChoice(saved, false)
        if (saved == null && ids.size == 1) return PlayerChoice(ids.single(), false)
        return PlayerChoice(null, ids.isNotEmpty())
    }
}

/** Bounded by ConnectionSetupDeadline outside this gate. Never retries source/playback commands. */
class ApprovalRetry {
    private var generation = 0L
    var active = false; private set
    fun begin(): Long { if (!active) { generation++; active = true }; return generation }
    fun clear() { generation++; active = false }
    fun current(ticket: Long) = active && ticket == generation
    fun mayCheck(ticket: Long, setupActive: Boolean, authorized: Boolean, requestPending: Boolean): Boolean =
        current(ticket) && setupActive && !authorized && !requestPending
}

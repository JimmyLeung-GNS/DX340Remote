package org.dx340.remote.protocol

/** A client recovery bound, independent of the host's pairing/authorization windows. */
class ConnectionSetupDeadline {
    companion object {
        const val TIMEOUT_MS = 60000L
        const val MESSAGE = "Connection setup expired. In Setup, find and connect to DX340 again. Open pairing on DX340 if approval is needed."
    }
    private var generation = 0L
    private var startedAt: Long? = null
    val active get() = startedAt != null
    fun begin(nowMs: Long): Long {
        require(nowMs >= 0)
        generation++; startedAt = nowMs
        return generation
    }
    fun clear() { generation++; startedAt = null }
    fun expired(nowMs: Long, ticket: Long? = null): Boolean {
        val start = startedAt ?: return false
        if (ticket != null && ticket != generation) return false
        // Compare elapsed time rather than adding a deadline near Long.MAX_VALUE.
        return nowMs < start || nowMs - start >= TIMEOUT_MS
    }
}

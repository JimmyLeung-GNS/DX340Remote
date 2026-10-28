package org.dx340.remote.protocol

/** Main-thread confirmed-frame queue. A lost callback closes it instead of stalling forever. */
class IndicationQueue(
    private val schedule: (Long, () -> Unit) -> (() -> Unit),
    private val submit: (ByteArray) -> String?,
    private val failed: (String) -> Unit,
    private val limit: Int = 12000,
    private val timeoutMs: Long = 10000
) {
    private val controls = ArrayDeque<ByteArray>()
    private val background = ArrayDeque<ByteArray>()
    private var active = false
    private var flight: Any? = null
    private var cancelTimeout: (() -> Unit)? = null
    val pendingCount get() = controls.size + background.size + if (flight != null) 1 else 0

    init { require(limit > 0 && timeoutMs > 0) }

    fun open() { active = true }

    fun enqueue(frames: List<ByteArray>, lowPriority: Boolean = false): Boolean {
        if (!active) return false
        if (frames.size > limit - pendingCount) { fail("OUTGOING_LIMIT"); return false }
        (if (lowPriority) background else controls).addAll(frames)
        pump()
        return active
    }

    /** Android supplies completion for the current indication; it supplies no frame ID. */
    fun complete(error: String? = null) {
        if (flight == null) return
        clearFlight()
        if (error != null) fail(error) else pump()
    }

    fun close() {
        active = false
        controls.clear(); background.clear(); clearFlight()
    }

    private fun clearFlight() {
        flight = null
        val cancel = cancelTimeout
        cancelTimeout = null
        cancel?.invoke()
    }

    private fun fail(reason: String) {
        close() // No callback may reenter a still-authorized queue or replay its old frames.
        failed(reason)
    }

    private fun pump() {
        if (!active || flight != null || (controls.isEmpty() && background.isEmpty())) return
        val bytes = if (controls.isNotEmpty()) controls.removeFirst() else background.removeFirst()
        val token = Any()
        flight = token
        cancelTimeout = schedule(timeoutMs) {
            if (flight === token) fail("INDICATION_TIMEOUT")
        }
        val error = try { submit(bytes) } catch (_: Exception) { "SEND_FAILED" }
        if (error != null && flight === token) fail(error)
    }
}

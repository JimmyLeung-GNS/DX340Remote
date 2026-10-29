package org.dx340.remote.protocol

/** One OS indication in flight across a bounded set of peripheral connections. Main thread only. */
class PeerIndicationQueue<P>(
    private val schedule: (Long, () -> Unit) -> (() -> Unit),
    private val submit: (P, ByteArray) -> String?,
    private val failed: (P, String) -> Unit,
    private val peerLimit: Int = 2,
    private val frameLimit: Int = 12000,
    private val timeoutMs: Long = 10000
) {
    private data class Frame<P>(val peer: P, val bytes: ByteArray)
    private class Flight<P>(val frame: Frame<P>, var cancel: (() -> Unit)? = null)
    private val peers = mutableSetOf<P>()
    private val controls = ArrayDeque<Frame<P>>()
    private val background = ArrayDeque<Frame<P>>()
    private var flight: Flight<P>? = null
    val pendingCount get() = controls.size + background.size + if (flight != null) 1 else 0
    val inFlightPeer get() = flight?.frame?.peer

    init { require(peerLimit > 0 && frameLimit > 0 && timeoutMs > 0) }

    fun open(peer: P): Boolean {
        if (peer !in peers && peers.size >= peerLimit) return false
        peers.add(peer); return true
    }
    fun enqueue(peer: P, frames: List<ByteArray>, lowPriority: Boolean = false): Boolean {
        if (peer !in peers) return false
        if (frames.size > frameLimit - pendingCount) { fail(peer, "OUTGOING_LIMIT"); return false }
        (if (lowPriority) background else controls).addAll(frames.map { Frame(peer, it) })
        pump(); return peer in peers
    }
    /** An already submitted value cannot be recalled. Drop only unsent values. */
    fun discardQueued(peer: P) {
        controls.removeAll { it.peer == peer }; background.removeAll { it.peer == peer }
    }
    fun close(peer: P) {
        peers.remove(peer); discardQueued(peer)
        // Retain a tombstone for this OS submission until callback/deadline, even
        // after disconnect. Otherwise a new device's frame could overlap it.
        pump()
    }
    fun complete(peer: P, error: String? = null) {
        val current = flight ?: return
        if (current.frame.peer != peer) return
        clearFlight()
        if (error != null && peer in peers) fail(peer, error) else pump()
    }
    fun stop() {
        peers.clear(); controls.clear(); background.clear(); clearFlight()
    }
    private fun clearFlight() {
        val previous = flight; flight = null; previous?.cancel?.invoke()
    }
    private fun fail(peer: P, reason: String) {
        close(peer); failed(peer, reason); pump()
    }
    private fun pump() {
        if (flight != null) return
        val frame = if (controls.isNotEmpty()) controls.removeFirst() else if (background.isNotEmpty()) background.removeFirst() else return
        if (frame.peer !in peers) { pump(); return }
        val token = Flight(frame); flight = token
        token.cancel = schedule(timeoutMs) {
            if (flight === token) {
                // A deadline is not an OS completion. Do not submit another
                // frame into a stack whose outstanding operation is unknown.
                // The transport must close/reset its server before reuse.
                stop()
                failed(frame.peer, "INDICATION_TIMEOUT")
            }
        }
        val error = try { submit(frame.peer, frame.bytes) } catch (_: Exception) { "SEND_FAILED" }
        if (error != null && flight === token) { clearFlight(); fail(frame.peer, error) }
    }
}

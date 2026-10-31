package org.dx340.remote.protocol

/** Main-thread FIFO: only the successful last frame completes a logical write. */
class AcknowledgedWriteQueue<T>(private val capacity: Int = 12000) {
    init { require(capacity in 1..12000) }
    private data class Frame<T>(val bytes: ByteArray, val completion: T?, var submission: T?)
    private val pending = ArrayDeque<Frame<T>>()
    private var flight: Frame<T>? = null
    val pendingCount get() = pending.size
    val busy get() = flight != null
    fun enqueue(frames: List<ByteArray>, completion: T? = null, submission: T? = null) {
        require(frames.isNotEmpty() && frames.size <= capacity - pending.size) { "Too many pending messages" }
        frames.forEachIndexed { i, bytes -> pending.addLast(Frame(bytes, completion.takeIf { i == frames.lastIndex }, submission.takeIf { i == 0 })) }
    }
    fun next(): ByteArray? {
        if (flight != null || pending.isEmpty()) return null
        return pending.removeFirst().also { flight = it }.bytes
    }
    fun acknowledge(): T? {
        val current = flight ?: return null
        flight = null
        return current.completion
    }
    /** Call only after the platform accepts the current write; never advances the FIFO. */
    fun submitted(): T? = flight?.let { frame -> frame.submission.also { frame.submission = null } }
    fun clear() { pending.clear(); flight = null }
}

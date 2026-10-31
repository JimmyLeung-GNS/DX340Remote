package org.dx340.remote.protocol

/** A successful, non-prepared ATT write proves this much payload fits this link. */
object PeerWriteCapacity {
    fun observe(current: Int, receivedFrameSize: Int): Int {
        require(current in 20..512 && receivedFrameSize in 9..512)
        return maxOf(current, receivedFrameSize)
    }
}

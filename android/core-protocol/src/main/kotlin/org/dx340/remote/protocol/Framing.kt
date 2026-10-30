package org.dx340.remote.protocol

import java.nio.ByteBuffer

object Wire {
    const val SERVICE = "e7e01000-4a76-4b53-9c8f-c7a12b00d340"
    const val INFO = "e7e01001-4a76-4b53-9c8f-c7a12b00d340"
    const val RX = "e7e01002-4a76-4b53-9c8f-c7a12b00d340"
    const val TX = "e7e01003-4a76-4b53-9c8f-c7a12b00d340"
    const val CCC = "00002902-0000-1000-8000-00805f9b34fb"
    const val MAX_MESSAGE = 65536
    const val HEADER = 8
    fun frames(data: ByteArray, capacity: Int, id: Int): List<ByteArray> {
        require(data.isNotEmpty() && data.size <= MAX_MESSAGE)
        require(capacity in 9..512 && id in 0..65535)
        val width = capacity - HEADER
        val count = (data.size + width - 1) / width
        require(count <= 65535) { "TOO_MANY_CHUNKS" }
        return (0 until count).map { index ->
            val part = data.copyOfRange(index * width, minOf((index + 1) * width, data.size))
            ByteBuffer.allocate(HEADER + part.size).put(0x44).put(1).putShort(id.toShort())
                .putShort(index.toShort()).putShort(count.toShort()).put(part).array()
        }
    }
}

class Reassembler(private val now: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private data class Partial(val count: Int, val start: Long, var lastProgress: Long, val pieces: MutableList<ByteArray>, var bytes: Int)
    private val partials = mutableMapOf<Int, Partial>()
    fun clear() = partials.clear()
    fun accept(frame: ByteArray): ByteArray? {
        val time = now()
        partials.entries.removeAll { time < it.value.lastProgress || time - it.value.lastProgress >= 10000 || time - it.value.start >= 60000 }
        require(frame.size in 9..512) { "MALFORMED_FRAME" }
        val b = ByteBuffer.wrap(frame)
        require(b.get() == 0x44.toByte() && b.get() == 1.toByte()) { "INCOMPATIBLE_PROTOCOL" }
        val id = b.short.toInt() and 65535
        val index = b.short.toInt() and 65535
        val count = b.short.toInt() and 65535
        require(count in 1..65535 && index < count) { "MALFORMED_FRAME" }
        val p = partials[id] ?: run {
            require(index == 0 && partials.size < 4) { "OUT_OF_ORDER" }
            Partial(count, time, time, mutableListOf(), 0).also { partials[id] = it }
        }
        if (p.count != count || index != p.pieces.size || p.bytes + b.remaining() > Wire.MAX_MESSAGE) {
            partials.remove(id); throw IllegalArgumentException("MALFORMED_FRAME")
        }
        val piece = ByteArray(b.remaining()); b.get(piece); p.pieces.add(piece); p.bytes += piece.size
        p.lastProgress = time
        if (p.pieces.size != count) return null
        partials.remove(id)
        return ByteArray(p.bytes).also { result -> var offset = 0; p.pieces.forEach { it.copyInto(result, offset); offset += it.size } }
    }
}

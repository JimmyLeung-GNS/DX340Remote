package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class PeerWriteCapacityTest {
    @Test fun restartedServerCanFrameAnAuthorizationReplyWithoutAnMtuCallback() {
        val hello = Wire.frames(ByteArray(223) { 1 }, 247, 1).single()
        val requestAssembler = Reassembler()
        assertNotNull(requestAssembler.accept(hello))
        val capacity = PeerWriteCapacity.observe(20, hello.size)
        val reply = ByteArray(6000) { (it % 251).toByte() }
        val frames = Wire.frames(reply, capacity, 2)
        assertTrue("Do not keep fragmenting at the default20 after a larger valid write", frames.size < 30)
        assertTrue(frames.all { it.size <= hello.size })
        val responseAssembler = Reassembler()
        frames.dropLast(1).forEach { assertNull(responseAssembler.accept(it)) }
        assertArrayEquals(reply, responseAssembler.accept(frames.last()))
    }

    @Test fun smallWritesPreserveKnownCapacityAndInvalidSizesCannotRaiseIt() {
        assertEquals(20, PeerWriteCapacity.observe(20, 9))
        assertEquals(512, PeerWriteCapacity.observe(512, 100))
        assertEquals(512, PeerWriteCapacity.observe(20, 512))
        assertThrows(IllegalArgumentException::class.java) { PeerWriteCapacity.observe(20, 513) }
        assertThrows(IllegalArgumentException::class.java) { PeerWriteCapacity.observe(20, 8) }
    }
}

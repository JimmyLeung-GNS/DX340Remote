package org.dx340.remote.protocol

import org.junit.Assert.*
import org.junit.Test

class ProtocolTest {
    @Test fun aDifferentSessionCannotBorrowMutationOwnershipUsingTheSameRequestId() {
        val gate = MutationGate(); gate.enter("shared-id")
        assertThrows(IllegalStateException::class.java) { gate.enter("shared-id") }
        gate.leave("other-id")
        assertThrows(IllegalStateException::class.java) { gate.enter("shared-id") }
        gate.leave("shared-id"); gate.enter("shared-id")
    }
    @Test fun asynchronousSelectionCannotInterleaveWithAnotherMutation() {
        val gate = MutationGate(); gate.enter("selection")
        assertThrows(IllegalStateException::class.java) { gate.enter("next") }
        gate.leave("unrelated")
        assertThrows(IllegalStateException::class.java) { gate.enter("next") }
        gate.leave("selection"); gate.enter("next")
    }
    @Test fun reconnectClearsUnsentControlsAndRequiresFreshAuthorization() {
        val session = ControllerSession(); session.hello("host1"); session.approve("host1"); session.register("skip1", "NEXT")
        session.lost(); assertTrue(session.pending.isEmpty()); assertFalse(session.authorized)
        assertThrows(IllegalArgumentException::class.java) { session.register("skip2", "NEXT") }
        session.hello("host2")
        assertThrows(IllegalArgumentException::class.java) { session.approve("host1") }
        session.approve("host2"); assertTrue(session.pending.isEmpty())
    }
    @Test fun rejectsParserDepthAttacksBeforeRecursiveJsonParser() {
        assertThrows(IllegalArgumentException::class.java) { JsonEnvelopeGuard.validate("{\"x\":" + "[".repeat(1000) + "0" + "]".repeat(1000) + "}") }
        assertThrows(IllegalArgumentException::class.java) { JsonEnvelopeGuard.validate("{\"x\":[}") }
        JsonEnvelopeGuard.validate("{\"title\":\"[braces] {inside} a string\",\"v\":1}")
    }
    @Test fun matchesSharedUtf8Fixtures() {
        val fixtures = org.json.JSONArray(javaClass.getResource("/framing.json")!!.readText())
        for (i in 0 until fixtures.length()) {
            val fixture = fixtures.getJSONObject(i)
            val frames = Wire.frames(fixture.getString("text").toByteArray(Charsets.UTF_8), fixture.getInt("capacity"), fixture.getInt("id"))
            val expected = fixture.getJSONArray("hex")
            assertEquals(expected.length(), frames.size)
            frames.forEachIndexed { index, bytes -> assertEquals(expected.getString(index), bytes.joinToString("") { "%02x".format(it.toInt() and 255) }) }
        }
    }
    @Test fun roundTripsAtAttBoundariesAndFullLimit() {
        for (capacity in listOf(20, 61, 244, 512)) for (size in listOf(1, 12, 13, 500, 65536)) {
            val bytes = ByteArray(size) { (it % 251).toByte() }; val assembler = Reassembler()
            val frames = Wire.frames(bytes, capacity, 65535)
            frames.dropLast(1).forEach { assertNull(assembler.accept(it)) }
            assertArrayEquals(bytes, assembler.accept(frames.last()))
        }
    }
    @Test fun rejectsOutOfOrderAndReleasesPartial() {
        val frames = Wire.frames(ByteArray(30), 20, 2); val assembler = Reassembler()
        assembler.accept(frames[0]); assertThrows(IllegalArgumentException::class.java) { assembler.accept(frames[2]) }
        assertNull(assembler.accept(frames[0]))
    }
    @Test fun expiresFragmentsAndClearsOnDisconnect() {
        var now = 0L; val assembler = Reassembler { now }; val frames = Wire.frames(ByteArray(30), 20, 3)
        assembler.accept(frames[0]); now = 10001
        assertThrows(IllegalArgumentException::class.java) { assembler.accept(frames[1]) }
        assembler.accept(frames[0]); assembler.clear()
        assertThrows(IllegalArgumentException::class.java) { assembler.accept(frames[1]) }
    }
    @Test fun rejectsOversizedAndMalformedMessages() {
        assertThrows(IllegalArgumentException::class.java) { Wire.frames(ByteArray(65537), 20, 0) }
        assertThrows(IllegalArgumentException::class.java) { Wire.frames(ByteArray(65536), 9, 0) }
        assertThrows(IllegalArgumentException::class.java) { Reassembler().accept(ByteArray(8)) }
        val assembler = Reassembler()
        for (i in 0..3) assembler.accept(Wire.frames(ByteArray(30), 20, i)[0])
        assertThrows(IllegalArgumentException::class.java) { assembler.accept(Wire.frames(ByteArray(30), 20, 5)[0]) }
    }
    @Test fun duplicateSkipExecutesOnlyOnceAndRequestReuseFails() {
        val ledger = CommandLedger(); var skips = 0
        repeat(2) { if (ledger.begin("abc", "NEXT") == null) { skips++; ledger.finish("abc", "dispatched") } }
        assertEquals(1, skips)
        assertThrows(IllegalArgumentException::class.java) { ledger.begin("abc", "PREVIOUS") }
    }
    @Test fun pendingAndBoundedLedgerCannotReplayEvictedCommands() {
        val ledger = CommandLedger(1); assertNull(ledger.begin("a", "NEXT")); assertEquals("PENDING", ledger.begin("a", "NEXT"))
        assertThrows(IllegalStateException::class.java) { ledger.begin("b", "NEXT") }
    }
    @Test fun rejectsStaleHostStaleSourceWrongTargetAndUnsupportedCapability() {
        assertThrows(IllegalArgumentException::class.java) { RoutingPolicy.validate("new", "old", "mango", "mango", "s", "s", true) }
        assertThrows(IllegalArgumentException::class.java) { RoutingPolicy.validate("h", "h", "mango", "apple", "s", "s", true) }
        assertThrows(IllegalArgumentException::class.java) { RoutingPolicy.validate("h", "h", "mango", "mango", "new", "old", true) }
        assertThrows(IllegalArgumentException::class.java) { RoutingPolicy.validate("h", "h", "mango", "mango", "s", "s", false) }
        RoutingPolicy.validate("h", "h", "mango", "mango", "s", "s", true)
    }
}

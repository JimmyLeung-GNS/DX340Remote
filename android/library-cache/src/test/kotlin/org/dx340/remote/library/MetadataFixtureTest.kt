package org.dx340.remote.library

import org.dx340.remote.protocol.Wire
import org.junit.Assert.*
import org.junit.Test

class MetadataFixtureTest {
    @Test fun allMetadataPagesKeepOpaqueIdentityAndFitProtocol() {
        val tracks = MetadataFixture.collection()
        assertEquals(10_000, tracks.size)
        assertTrue(tracks.any { it.artist.isEmpty() && it.format == "WAV" })
        assertTrue(tracks.groupBy { it.title }.values.any { it.size > 1 })
        val received = mutableListOf<String>()
        for (page in 0 until 400) {
            val bytes = MetadataFixture.encode(page, MetadataFixture.page(tracks, page))
            assertTrue(bytes.size <= Wire.MAX_MESSAGE)
            val data = MetadataFixture.decode(bytes)
            val rows = data.getJSONArray("items")
            assertEquals(25, rows.length())
            for (row in 0 until rows.length()) received += rows.getJSONObject(row).getString("id")
            if (page < 399) assertEquals(page + 1, data.getInt("nextPage")) else assertTrue(data.isNull("nextPage"))
            if (page in listOf(0, 199, 399)) for (capacity in listOf(20, 185, 512)) {
                assertArrayEquals(bytes, MetadataFixture.frameRoundTrip(bytes, capacity))
            }
        }
        assertEquals(tracks.map { it.id }, received)
        assertEquals(10_000, received.toSet().size)
        assertTrue(MetadataFixture.page(tracks, 400).isEmpty())
    }

    @Test fun metadataCacheHonorsRecencySourceAndRevisionBoundaries() {
        val tracks = MetadataFixture.collection()
        val cache = LibraryIndex<MetadataFixture.Track>(200)
        for (page in 0 until 200) cache.put(MetadataFixture.key(page), MetadataFixture.page(tracks, page))
        assertNotNull(cache.get(MetadataFixture.key(0))) // Refresh LRU order.
        cache.put(MetadataFixture.key(200), MetadataFixture.page(tracks, 200))
        assertNull(cache.get(MetadataFixture.key(1)))
        assertNotNull(cache.get(MetadataFixture.key(0)))
        assertNull(cache.get(MetadataFixture.key(0, "fixture-r2")))
        val otherSource = MetadataFixture.key(0).copy(source = "synthetic:other")
        cache.put(otherSource, MetadataFixture.page(tracks, 0))
        cache.invalidate(MetadataFixture.SOURCE)
        assertNull(cache.get(MetadataFixture.key(0)))
        assertEquals(tracks[0], cache.get(otherSource)?.first())
        assertThrows(IllegalArgumentException::class.java) { cache.put(otherSource, tracks.take(51)) }
    }

    @Test fun revisionGuardDetectsReplacementDespiteUnchangedDisplayTitle() {
        val original = MetadataFixture.collection().first()
        val replacement = original.copy(id = "replacement-id", album = "Different album")
        val guard = RevisionGuard()
        val key = "${MetadataFixture.SOURCE}\u0000songs\u00000"
        assertFalse(guard.update(key, mapOf(original.id to original.fingerprint())))
        assertFalse(guard.update(key, mapOf(original.id to original.fingerprint())))
        assertTrue(guard.update(key, mapOf(replacement.id to replacement.fingerprint())))
        assertThrows(IllegalArgumentException::class.java) { guard.validateSelection(original.fingerprint(), replacement.fingerprint()) }
        assertThrows(IllegalArgumentException::class.java) { guard.validateSelection(original.fingerprint(), null) }
    }
}

package org.dx340.remote.library
import org.junit.Assert.*
import org.junit.Test

class LibraryIndexTest {
    @Test fun positionalSourceIdsCannotSilentlySelectChangedItems() {
        val guard = RevisionGuard()
        assertFalse(guard.update("mango:root:0", mapOf("5" to "song-A")))
        assertFalse(guard.update("apple:root:0", mapOf("5" to "song-B")))
        assertTrue(guard.update("mango:root:0", mapOf("5" to "song-B")))
        assertThrows(IllegalArgumentException::class.java) { guard.validateSelection("song-A", "song-B") }
        assertThrows(IllegalArgumentException::class.java) { guard.validateSelection("song-A", null) }
    }
    @Test fun revisionAndSourceArePartOfCacheIdentity() {
        val cache = LibraryIndex<Int>(); val key = LibraryIndex.Key("mango", "r1", "root", 0)
        cache.put(key, listOf(1)); assertNull(cache.get(key.copy(revision = "r2"))); assertNull(cache.get(key.copy(source = "apple")))
        cache.invalidate("mango"); assertNull(cache.get(key))
    }
    @Test fun handlesTenThousandTracksWithBoundedPages() {
        val cache = LibraryIndex<Int>(200)
        for (page in 0 until 400) cache.put(LibraryIndex.Key("mango", "r", "root", page), (page * 25 until page * 25 + 25).toList())
        assertNull(cache.get(LibraryIndex.Key("mango", "r", "root", 0)))
        assertEquals(9999, cache.get(LibraryIndex.Key("mango", "r", "root", 399))?.last())
    }
}

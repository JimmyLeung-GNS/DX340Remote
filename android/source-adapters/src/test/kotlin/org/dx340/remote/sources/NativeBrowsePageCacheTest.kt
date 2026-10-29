package org.dx340.remote.sources

import org.dx340.remote.library.LibraryIndex
import org.junit.Assert.*
import org.junit.Test

class NativeBrowsePageCacheTest {
    private val key = LibraryIndex.Key("mango", "epoch|package-version", "albums", 0)
    private fun row(id: String) = NativeBrowsePageCache.Row(id, "曲 $id", "Artist", false, true, "identity-$id")

    @Test fun cachedPresentationCannotReplaceDeliberatelyFreshPlaybackValidation() {
        var now = 1_000L
        val cache = NativeBrowsePageCache({ now })
        cache.remember(key, listOf(row("5")), 1)
        assertEquals("5", cache.read(key, true) { it.fingerprint == "identity-5" }!!.rows.single().id)
        var proofLookups = 0
        assertNull(cache.read(key, false) { proofLookups++; true })
        assertEquals(0, proofLookups)
        assertNull(cache.read(key, true) { false }) // Evicted/changed item proof is not restored.
        now += 9_999
        assertNotNull(cache.read(key, true) { true })
        now++
        assertNull(cache.read(key, true) { true }) // Ten seconds is expired, not inclusive.
        now = 999
        assertNull(cache.read(key, true) { true }) // A backwards clock is not fresh.
    }

    @Test fun sourceEpochVersionParentAndPageAreIndependentAndInvalidationKeepsOtherSources() {
        val cache = NativeBrowsePageCache({ 0L })
        cache.remember(key, listOf(row("5")), null)
        for (other in listOf(key.copy(source = "apple"), key.copy(revision = "epoch2|package-version"),
            key.copy(revision = "epoch|new-package"), key.copy(parent = "artists"), key.copy(page = 1))) {
            assertNull(cache.read(other, true) { true })
        }
        val apple = key.copy(source = "apple")
        cache.remember(apple, listOf(row("5")), null)
        cache.invalidate("mango")
        assertNull(cache.read(key, true) { true })
        assertNotNull(cache.read(apple, true) { true })
    }

    @Test fun emptyFinalPageAndCursorSurviveWithoutInferringNextFromRowCount() {
        val cache = NativeBrowsePageCache({ 0L })
        val input = mutableListOf(row("5"))
        cache.remember(key, input, 1)
        input.clear()
        assertEquals(1, cache.read(key, true) { true }!!.nextPage)
        assertEquals(1, cache.read(key, true) { true }!!.rows.size)
        val terminal = key.copy(page = 1)
        cache.remember(terminal, emptyList(), null)
        val last = cache.read(terminal, true) { error("An empty page needs no item proof") }!!
        assertTrue(last.rows.isEmpty()); assertNull(last.nextPage)
    }

    @Test fun tenThousandMetadataRowsUseAtMostTwoHundredPagesAndRespectLruRecency() {
        val cache = NativeBrowsePageCache({ 0L })
        for (page in 0 until 200) cache.remember(key.copy(page = page), (0 until 25).map { row("${page * 25 + it}") }, page + 1)
        assertNotNull(cache.read(key, true) { true })
        cache.remember(key.copy(page = 200), (5000 until 5025).map { row("$it") }, 201)
        assertNull(cache.read(key.copy(page = 1), true) { true })
        assertNotNull(cache.read(key, true) { true })
        for (page in 201 until 400) cache.remember(key.copy(page = page), (0 until 25).map { row("${page * 25 + it}") }, if (page == 399) null else page + 1)
        // The touched first page remains recent; the untouched page200 is evicted.
        assertNotNull(cache.read(key, true) { true })
        assertNull(cache.read(key.copy(page = 200), true) { true })
        assertEquals("9999", cache.read(key.copy(page = 399), true) { true }!!.rows.last().id)
    }

    @Test fun malformedOrUnboundedSourceMetadataCannotEnterPresentationCache() {
        val cache = NativeBrowsePageCache({ 0L })
        for (rows in listOf(List(26) { row("$it") }, listOf(row("5"), row("5")),
            listOf(row("a").copy(id = "字".repeat(171))), listOf(row("a").copy(title = "t".repeat(129))),
            listOf(row("a").copy(subtitle = "s".repeat(65))))) {
            assertThrows(IllegalArgumentException::class.java) { cache.remember(key, rows, null) }
        }
        assertThrows(IllegalArgumentException::class.java) { cache.remember(key, listOf(row("5")), 9) }
        assertNull(cache.read(key, true) { true })
    }
}

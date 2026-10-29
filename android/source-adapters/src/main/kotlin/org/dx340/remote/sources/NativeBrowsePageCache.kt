package org.dx340.remote.sources

import org.dx340.remote.library.LibraryIndex

/** Presentation metadata only. A cache hit is never evidence authorizing playback. */
class NativeBrowsePageCache(
    private val now: () -> Long,
    capacity: Int = 200,
    private val maxAgeMs: Long = 10_000
) {
    data class Row(val id: String, val title: String, val subtitle: String,
                   val browsable: Boolean, val playable: Boolean, val fingerprint: String)
    data class Page(val rows: List<Row>, val nextPage: Int?, val observedAt: Long)
    private val pages = LibraryIndex<Page>(capacity)

    init { require(capacity > 0 && maxAgeMs > 0) }

    fun remember(key: LibraryIndex.Key, rows: List<Row>, nextPage: Int?) {
        require(rows.size <= 25 && rows.map { it.id }.distinct().size == rows.size)
        require(rows.all { it.id.toByteArray(Charsets.UTF_8).size <= 512 && it.title.length <= 128 && it.subtitle.length <= 64 })
        require(nextPage == null || nextPage == key.page + 1)
        pages.put(key, listOf(Page(rows.toList(), nextPage, now())))
    }

    fun read(key: LibraryIndex.Key, allowCached: Boolean, hasCurrentProof: (Row) -> Boolean): Page? {
        if (!allowCached) return null
        val page = pages.get(key)?.singleOrNull() ?: return null
        val age = now() - page.observedAt
        // Source identity/proofs can be evicted independently; never recreate them from cache.
        if (age < 0 || age >= maxAgeMs || page.rows.any { !hasCurrentProof(it) }) return null
        return page
    }

    fun invalidate(source: String) = pages.invalidate(source)
}

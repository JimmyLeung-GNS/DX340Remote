package org.dx340.remote.library

/** Bounded metadata snapshots. IDs are source-owned, never filesystem paths. */
class LibraryIndex<T>(private val capacity: Int = 200) {
    data class Key(val source: String, val revision: String, val parent: String, val page: Int)
    private val pages = object : LinkedHashMap<Key, List<T>>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, List<T>>?) = size > capacity
    }
    @Synchronized fun put(key: Key, items: List<T>) { require(items.size <= 50); pages[key] = items.toList() }
    @Synchronized fun get(key: Key): List<T>? = pages[key]
    @Synchronized fun invalidate(source: String) { pages.keys.removeAll { it.source == source } }
}

package org.dx340.remote.library

class RevisionGuard {
    private val snapshots = object : LinkedHashMap<String, Map<String, String>>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, String>>?) = size > 400
    }
    fun update(page: String, items: Map<String, String>): Boolean {
        val previous = snapshots.put(page, items.toMap())
        return previous != null && previous != items
    }
    fun validateSelection(original: String, current: String?) {
        require(current != null) { "EXPIRED_ITEM" }
        require(original == current) { "STALE_LIBRARY" }
    }
    fun clear() { snapshots.clear() }
}

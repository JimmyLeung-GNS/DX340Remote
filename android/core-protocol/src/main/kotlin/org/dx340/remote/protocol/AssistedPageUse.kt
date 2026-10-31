package org.dx340.remote.protocol

/** An assisted selection consumes source UI action IDs, not the user's controller grant. */
class AssistedPageUse {
    companion object {
        fun tracks(source: String, item: String, assisted: Boolean, requestedRevision: String, pageRevision: String): Boolean {
            val sourceItem = source == "com.ibasso.music" && item.startsWith("mango-ui:") ||
                source == "com.apple.android.music" && item.startsWith("ui:")
            return sourceItem && assisted && pageRevision.isNotEmpty() && requestedRevision == pageRevision
        }
    }
    private data class Use(val id: String, val source: String, val epoch: String, val revision: String)
    private var pending: Use? = null
    fun begin(id: String, source: String, epoch: String, revision: String, assisted: Boolean) {
        pending = if (assisted && id.isNotEmpty() && revision.isNotEmpty()) Use(id, source, epoch, revision) else null
    }
    fun clear() { pending = null }
    fun abandon(source: String, epoch: String, revision: String): Boolean =
        pending?.let { finish(it.id, source, epoch, revision) } ?: false
    /** Consume only a terminal reply/timeout for this request; never retire a newer page. */
    fun finish(id: String, source: String, epoch: String, revision: String): Boolean {
        val use = pending ?: return false
        if (id != use.id) return false
        pending = null
        return source == use.source && epoch == use.epoch && revision == use.revision
    }
}

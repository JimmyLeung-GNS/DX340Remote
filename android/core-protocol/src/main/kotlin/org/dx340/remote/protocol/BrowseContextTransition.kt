package org.dx340.remote.protocol

data class BrowseContext(val source: String, val epoch: String, val mode: String, val assistedReady: Boolean)
enum class BrowseReset { NONE, ALL, ASSISTED }
data class BrowseInvalidation(val reset: BrowseReset, val message: String?)

/** Presentation invalidation only. Never dispatches a request or retries playback. */
object BrowseContextTransition {
    fun evaluate(old: BrowseContext, new: BrowseContext, hasLibrary: Boolean, hasQueue: Boolean,
                 assistedLibrary: Boolean, notify: Boolean): BrowseInvalidation = when {
        old.source != new.source || old.epoch != new.epoch || old.mode != new.mode -> BrowseInvalidation(
            BrowseReset.ALL, if (notify && (hasLibrary || hasQueue)) "Library changed. Browse again or refresh Queue." else null)
        old.assistedReady != new.assistedReady && assistedLibrary -> BrowseInvalidation(
            BrowseReset.ASSISTED, if (notify && hasLibrary) "Assisted browsing changed. Browse again; wake/unlock DX340 and check browsing access if needed." else null)
        else -> BrowseInvalidation(BrowseReset.NONE, null)
    }
}

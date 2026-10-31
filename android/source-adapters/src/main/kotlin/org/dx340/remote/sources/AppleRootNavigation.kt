package org.dx340.remote.sources

/** Coordinates read-only root navigation; never admits an item or issues an action. */
class AppleRootNavigation {
    enum class Action { DOWNLOADS, BACK, LIBRARY }
    enum class Decision { WAIT, NAVIGATE, PUBLISH }
    private data class Context(val root: Boolean, val mainLibrary: Boolean, val title: String, val rows: List<UiRow>)
    private var pending: Action? = null
    private var origin: Context? = null
    private var candidate: Context? = null
    private var since = 0L
    private var rootSeen = false
    // Paths/fingerprints still bind actual actions in the service. They are not
    // evidence that a navigation destination changed while a toolbar animates.
    private fun context(page: ParsedAppleUi?, downloadedEntry: Boolean): Context? = when {
        page?.downloadedRoot == true -> Context(true, false, page.title, page.rows.map { it.copy(path = "") })
        downloadedEntry -> Context(false, true, "", emptyList())
        page != null -> Context(false, false, page.title, page.rows.map { it.copy(path = "") })
        else -> null
    }
    private fun stable(value: Context, now: Long): Boolean {
        if (candidate != value || now < since) { candidate = value; since = now; return false }
        return now - since >= 450
    }
    fun started(action: Action, page: ParsedAppleUi?) {
        pending = action; origin = context(page, false); candidate = null; rootSeen = false
    }
    fun observe(page: ParsedAppleUi?, downloadedEntry: Boolean, now: Long): Decision {
        val current = context(page, downloadedEntry)
        if (page?.downloadedRoot == true) {
            rootSeen = true
            return if (stable(current!!, now)) Decision.PUBLISH else Decision.WAIT
        }
        if (rootSeen || pending == Action.DOWNLOADS) { candidate = null; return Decision.WAIT }
        if (pending == null) { candidate = null; return Decision.NAVIGATE }
        val progressed = downloadedEntry || current != null && current != origin
        if (!progressed) { candidate = null; return Decision.WAIT }
        if (!stable(current!!, now)) return Decision.WAIT
        pending = null; candidate = null
        return Decision.NAVIGATE
    }
}

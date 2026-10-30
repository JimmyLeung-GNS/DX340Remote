package org.dx340.remote.sources

data class UiWindow(val id: Int, val type: Int, val layer: Int, val active: Boolean, val focused: Boolean)

/** Public AccessibilityWindowInfo types: application1, input-method2, accessibility-overlay4. */
object UiWindowGuard {
    fun permits(rootId: Int, windows: List<UiWindow>): Boolean {
        val root = windows.singleOrNull { it.id == rootId } ?: return false
        if (windows.size > 64 || windows.map { it.id }.distinct().size != windows.size || root.type != 1 || !root.active) return false
        return windows.none { it.id != rootId && (it.focused || it.layer >= root.layer && it.type in setOf(1, 2, 4)) }
    }
}

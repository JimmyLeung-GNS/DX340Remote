package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class UiWindowGuardTest {
    private val root = UiWindow(8, 1, 2, active = true, focused = true)
    @Test fun unobscuredApplicationWithSystemBarsIsPermitted() {
        assertTrue(UiWindowGuard.permits(8, listOf(root, UiWindow(9, 3, 5, false, false))))
    }
    @Test fun nonFocusablePopupAboveActiveLibraryIsRejected() {
        assertFalse(UiWindowGuard.permits(8, listOf(root, UiWindow(9, 1, 3, false, false))))
    }
    @Test fun keyboardAccessibilityOverlayOrAnotherFocusedWindowIsRejected() {
        for (type in listOf(2, 4)) assertFalse(UiWindowGuard.permits(8, listOf(root, UiWindow(9, type, 3, false, false))))
        assertFalse(UiWindowGuard.permits(8, listOf(root, UiWindow(9, 3, 1, false, true))))
    }
    @Test fun missingInactiveAmbiguousOrUnverifiedRootFailsClosed() {
        assertFalse(UiWindowGuard.permits(8, emptyList()))
        assertFalse(UiWindowGuard.permits(8, listOf(root.copy(active = false))))
        assertFalse(UiWindowGuard.permits(8, listOf(root, root)))
        assertFalse(UiWindowGuard.permits(8, listOf(root.copy(type = 3))))
        assertFalse(UiWindowGuard.permits(8, listOf(root, UiWindow(9, 1, 2, false, false))))
    }
}

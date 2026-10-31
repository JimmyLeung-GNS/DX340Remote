package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test
import org.dx340.remote.sources.AppleRootNavigation.Action
import org.dx340.remote.sources.AppleRootNavigation.Decision

class AppleRootNavigationTest {
    private fun page(title: String, row: String = "Track", path: String = "0.2", root: Boolean = false) =
        ParsedAppleUi(title, listOf(UiRow(path, row, "", !root, root)), "0.1", null, root, path)
    @Test fun downloadsIsNotClickedAgainWhileOriginOrUnknownFrameRemains() {
        val nav = AppleRootNavigation(); nav.started(Action.DOWNLOADS, null)
        assertEquals(Decision.WAIT, nav.observe(null, true, 0))
        assertEquals(Decision.WAIT, nav.observe(null, false, 1000))
    }
    @Test fun rootMustBeVerifiedAndStableBeforePublishing() {
        val nav = AppleRootNavigation(); val root = page("Downloaded", root = true)
        nav.started(Action.DOWNLOADS, null)
        assertEquals(Decision.WAIT, nav.observe(root, false, 0))
        assertEquals(Decision.WAIT, nav.observe(root, false, 449))
        assertEquals(Decision.PUBLISH, nav.observe(root, false, 450))
    }
    @Test fun backDoesNotRepeatForToolbarOrRowPathMovement() {
        val nav = AppleRootNavigation(); nav.started(Action.BACK, page("Songs"))
        assertEquals(Decision.WAIT, nav.observe(page("Songs", path = "0.3"), false, 1000))
    }
    @Test fun nextBackWaitsForStableParentContext() {
        val nav = AppleRootNavigation(); nav.started(Action.BACK, page("Album"))
        assertEquals(Decision.WAIT, nav.observe(page("Albums"), false, 0))
        assertEquals(Decision.NAVIGATE, nav.observe(page("Albums"), false, 450))
        nav.started(Action.BACK, page("Albums"))
        assertEquals(Decision.WAIT, nav.observe(null, true, 500))
        assertEquals(Decision.NAVIGATE, nav.observe(null, true, 950))
    }
    @Test fun changingCandidateResetsItsSettlingInterval() {
        val nav = AppleRootNavigation(); nav.started(Action.BACK, page("Album"))
        assertEquals(Decision.WAIT, nav.observe(page("Albums", row = "A"), false, 0))
        assertEquals(Decision.WAIT, nav.observe(page("Albums", row = "B"), false, 400))
        assertEquals(Decision.WAIT, nav.observe(page("Albums", row = "B"), false, 500))
        assertEquals(Decision.NAVIGATE, nav.observe(page("Albums", row = "B"), false, 850))
    }
    @Test fun libraryWaitsForAnIdentifiedDestination() {
        val nav = AppleRootNavigation(); nav.started(Action.LIBRARY, null)
        assertEquals(Decision.WAIT, nav.observe(null, false, 0))
        assertEquals(Decision.WAIT, nav.observe(null, true, 500))
        assertEquals(Decision.NAVIGATE, nav.observe(null, true, 950))
        nav.started(Action.LIBRARY, page("Songs"))
        assertEquals(Decision.WAIT, nav.observe(page("Songs"), false, 2000))
    }
    @Test fun unknownFramesCannotCompleteAnUnknownBack() {
        val nav = AppleRootNavigation(); nav.started(Action.BACK, null)
        assertEquals(Decision.WAIT, nav.observe(null, false, 500))
        assertEquals(Decision.WAIT, nav.observe(page("Albums"), false, 1000))
        assertEquals(Decision.NAVIGATE, nav.observe(page("Albums"), false, 1450))
    }
    @Test fun newRequestHasNoPendingActionAndClockRegressionDoesNotPublish() {
        val nav = AppleRootNavigation(); assertEquals(Decision.NAVIGATE, nav.observe(null, false, 0))
        val root = page("Downloaded", root = true)
        assertEquals(Decision.WAIT, nav.observe(root, false, 1000))
        assertEquals(Decision.WAIT, nav.observe(root, false, 900))
        assertEquals(Decision.WAIT, nav.observe(root, false, 1300))
        assertEquals(Decision.PUBLISH, nav.observe(root, false, 1350))
    }
    @Test fun losingRootDuringSettlingDoesNotNavigateAway() {
        val nav = AppleRootNavigation(); val root = page("Downloaded", root = true)
        assertEquals(Decision.WAIT, nav.observe(root, false, 0))
        assertEquals(Decision.WAIT, nav.observe(null, false, 250))
        assertEquals(Decision.WAIT, nav.observe(root, false, 500))
        assertEquals(Decision.PUBLISH, nav.observe(root, false, 950))
    }
}

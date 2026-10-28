package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class AppleUiModelTest {
    private val tab = UiNode("0.0", "0", "com.apple.android.music:id/navigation_bar_item_large_label_view", "Library", selected = true)
    @Test fun exposesOnlyDownloadedSongRowsAsPlayable() {
        val nodes = listOf(tab,
            UiNode("0.1", "0", "com.apple.android.music:id/collection_header_title", "Synthetic album"),
            UiNode("0.2", "0", "com.apple.android.music:id/recycler_view", forward = true),
            UiNode("0.2.0", "0.2", "com.apple.android.music:id/collection_list_item", clickable = true),
            UiNode("0.2.0.0", "0.2.0", "com.apple.android.music:id/title", "Downloaded fixture"),
            UiNode("0.2.0.1", "0.2.0", "com.apple.android.music:id/track_action_button", description = "Downloaded Song"),
            UiNode("0.2.1", "0.2", "com.apple.android.music:id/collection_list_item", clickable = true),
            UiNode("0.2.1.0", "0.2.1", "com.apple.android.music:id/title", "Cloud fixture"))
        val parsed = AppleUiModel.parse(nodes, true)
        assertTrue(parsed.rows[0].playable); assertFalse(parsed.rows[1].playable); assertFalse(parsed.rows[1].browsable)
        val changed = nodes.map { if (it.text == "Downloaded fixture") it.copy(text = "Different track") else it }
        assertNotEquals(parsed.fingerprint, AppleUiModel.parse(changed, true).fingerprint)
        val collapsed = nodes.map { if (it.id.endsWith("/collection_header_title")) it.copy(id = "com.apple.android.music:id/main_title") else it }
        assertEquals(parsed.fingerprint, AppleUiModel.parse(collapsed, true).fingerprint)
    }
    @Test fun rejectsUnknownScreenAndUnverifiedOfflineContext() {
        assertThrows(IllegalArgumentException::class.java) { AppleUiModel.parse(listOf(tab.copy(selected = false)), true) }
        assertThrows(IllegalArgumentException::class.java) { AppleUiModel.parse(listOf(tab), false) }
    }
    @Test fun recognizesObservedPlaylistRowsOnlyInsideVerifiedList() {
        val nodes = listOf(tab,
            UiNode("0.1", "0", "com.apple.android.music:id/toolbar_title", "Playlists"),
            UiNode("0.2", "0", "com.apple.android.music:id/library_details_list"),
            UiNode("0.2.0", "0.2", className = "android.view.ViewGroup", clickable = true),
            UiNode("0.2.0.0", "0.2.0", text = "Fixture playlist", description = "Fixture playlist", className = "android.widget.TextView"),
            UiNode("0.3", "0", text = "New Playlist", description = "New Playlist", className = "android.widget.TextView", clickable = true))
        val parsed = AppleUiModel.parse(nodes, true)
        assertEquals(listOf("Fixture playlist"), parsed.rows.map { it.title })
        assertTrue(parsed.rows.single().browsable); assertFalse(parsed.rows.single().playable)
        assertThrows(IllegalArgumentException::class.java) { AppleUiModel.parse(nodes, false) }
    }
    @Test fun rootOffersSemanticCategoriesWithoutPlayButtonsOrRecentlyAdded() {
        val root = listOf(tab, UiNode("0.1", "0", text = "Only showing music on this device."),
            UiNode("0.2", "0", clickable = true), UiNode("0.2.0", "0.2", "library_categories-Albums", "Albums"),
            UiNode("0.3", "0", text = "Play", clickable = true))
        val parsed = AppleUiModel.parse(root, false)
        assertEquals(listOf("Albums"), parsed.rows.map { it.title }); assertTrue(parsed.downloadedRoot); assertNull(parsed.back)
        val merged = root.map { if (it == tab) it.copy(id = "com.apple.android.music:id/action_library", text = "", description = "Library") else it }
        assertTrue(AppleUiModel.parse(merged, false).downloadedRoot)
    }
}

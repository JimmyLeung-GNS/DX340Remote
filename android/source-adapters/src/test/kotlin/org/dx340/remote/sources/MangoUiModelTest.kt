package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class MangoUiModelTest {
    private val p = "com.ibasso.music:id/"
    private fun playlists() = listOf(
        UiNode("0.0", "0", p + "nav_bar"),
        UiNode("0.0.0", "0.0", p + "nav_bar_back_iv", clickable = true),
        UiNode("0.0.1", "0.0", p + "entry_title_tv", "Playlist"),
        UiNode("0.1", "0", p + "add_new_playlist_view", "New Playlist", clickable = true),
        UiNode("0.2", "0", p + "playlist_view"))
    @Test fun playlistRecoveryOffersOnlyTheObservedMenuArrow() {
        val nodes = playlists()
        assertEquals("0.0.0", MangoUiModel.playlistMenuAction(nodes)!!.path)
        assertNull(MangoUiModel.playlistMenuAction(nodes.map { if (it.text == "Playlist") it.copy(text = "Settings") else it }))
        assertNull(MangoUiModel.playlistMenuAction(folder()))
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.parse(nodes, "internal") }
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.playlistMenuAction(nodes.map { if (it.id == p + "nav_bar_back_iv") it.copy(enabled = false) else it }) }
    }
    @Test fun playlistRecoveryRejectsCachedOrUnrelatedPageControls() {
        val nodes = playlists()
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.playlistMenuAction(nodes + UiNode("0.3", "0", p + "entry_title_tv", "Internal Storage")) }
        assertThrows(IllegalStateException::class.java) { MangoUiModel.playlistMenuAction(nodes + UiNode("0.3", "0", p + "playlist_view")) }
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.playlistMenuAction(nodes.map { if (it.id == p + "playlist_view") it.copy(parent = "other-page") else it }) }
    }
    private fun row(page: Int, index: Int, title: String, file: Boolean = true) = listOf(
        UiNode("0.2.$page.0.$index", "0.2.$page.0", clickable = true),
        UiNode("0.2.$page.0.$index.0", "0.2.$page.0.$index", p + if (file) "file_info_index_tv" else "file_info_icon_iv", if (file) "1" else ""),
        UiNode("0.2.$page.0.$index.1", "0.2.$page.0.$index", p + "file_name_tv", title))
    private fun folder() = listOf(
        UiNode("0.1", "0", p + "entry_title_tv", "Internal Storage"),
        UiNode("0.2", "0", p + "folder_detail_content"),
        UiNode("0.2.0", "0.2", p + "view_root", drawingOrder = 2),
        UiNode("0.2.0.0", "0.2.0", p + "music_lib_all_music_list", forward = true))
    @Test fun excludesOverlappingCachedAncestorsAndRestrictsFileActions() {
        val nodes = folder() + row(0, 0, "Fixture.flac") + row(0, 1, "Fixture.WAV") + row(0, 2, "Album", false) + row(0, 3, "Other.mp3") +
            listOf(UiNode("0.2.1", "0.2", p + "view_root", drawingOrder = 1), UiNode("0.2.1.0", "0.2.1", p + "music_lib_all_music_list")) + row(1, 0, "Hidden.flac")
        val parsed = MangoUiModel.parse(nodes, "internal")
        assertEquals(listOf("Fixture.flac", "Fixture.WAV", "Album", "Other.mp3"), parsed.rows.map { it.title })
        assertTrue(parsed.rows[0].playable); assertTrue(parsed.rows[1].playable); assertTrue(parsed.rows[2].browsable)
        assertFalse(parsed.rows[3].playable); assertFalse(parsed.rows[3].browsable)
    }
    @Test fun changedFileAtSamePathChangesSnapshotAndNumericOrderIsPreserved() {
        val nodes = folder() + row(0, 10, "Ten.flac") + row(0, 2, "Two.wav")
        val a = MangoUiModel.parse(nodes, "internal")
        assertEquals(listOf("Two.wav", "Ten.flac"), a.rows.map { it.title })
        assertNotEquals(a.fingerprint, MangoUiModel.parse(nodes.map { if (it.text == "Two.wav") it.copy(text = "Replacement.wav") else it }, "internal").fingerprint)
    }
    @Test fun storageAndMenuContextFailClosed() {
        val nodes = folder() + row(0, 0, "Fixture.flac")
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.parse(nodes, "sd") }
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.parse(nodes, "network") }
        val menu = listOf(UiNode("0.8", "0", p + "internalStorgeLayout"), UiNode("0.9", "0", p + "sdcardLayout"))
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.parse(nodes + menu, "internal") }
    }
    @Test fun menuOnlyOffersKnownLocalStorageActions() {
        val nodes = listOf(UiNode("0.1", "0", p + "internalStorgeLayout", clickable = true), UiNode("0.1.0", "0.1", text = "Internal Storage"),
            UiNode("0.2", "0", p + "sdcardLayout", clickable = true), UiNode("0.2.0", "0.2", text = "SD Card"), UiNode("0.3", "0", p + "netLayout", "Local Network", clickable = true))
        assertEquals("0.1", MangoUiModel.storageAction(nodes, "internal").path)
        assertEquals("0.2", MangoUiModel.storageAction(nodes, "sd").path)
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.storageAction(nodes, "network") }
    }
    @Test fun breadcrumbParentIsIndependentOfMenuArrowAndChangesIdentity() {
        val nodes = folder() + row(0, 0, "Fixture.flac") + listOf(
            UiNode("0.3", "0", clickable = true), UiNode("0.3.0", "0.3", p + "tv_content", "Internal Storage"),
            UiNode("0.4", "0", clickable = true), UiNode("0.4.0", "0.4", p + "tv_content", "Music"),
            UiNode("0.5", "0", clickable = true), UiNode("0.5.0", "0.5", p + "tv_content", "Album"),
            UiNode("0.6", "0", p + "nav_bar_back_iv", clickable = true))
        val parsed = MangoUiModel.parse(nodes, "internal")
        assertEquals("0.4", parsed.back); assertEquals("0.3", parsed.root)
        assertNotEquals(parsed.fingerprint, MangoUiModel.parse(nodes.map { if (it.text == "Album") it.copy(text = "Other album") else it }, "internal").fingerprint)
    }
    @Test fun missingActivePageAndAmbiguousRowsAreRejected() {
        assertEquals("UI_UNEXPECTED_SCREEN", assertThrows(RuntimeException::class.java) { MangoUiModel.parse(folder().filterNot { it.path == "0.2.0" } + row(0, 0, "Fixture.flac"), "internal") }.message)
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.parse(folder() + row(0, 0, "Fixture.flac") + UiNode("0.2.0.0.0.2", "0.2.0.0.0", p + "file_info_icon_iv"), "internal") }
    }
    @Test fun drawingOrderChoosesTopPageAndAmbiguityFailsClosed() {
        val nodes = folder() + row(0, 0, "Hidden.wav") + listOf(UiNode("0.2.1", "0.2", p + "view_root", drawingOrder = 3), UiNode("0.2.1.0", "0.2.1", p + "music_lib_all_music_list")) + row(1, 0, "Visible.flac")
        assertEquals(listOf("Visible.flac"), MangoUiModel.parse(nodes, "internal").rows.map { it.title })
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.parse(nodes.map { if (it.path == "0.2.1") it.copy(drawingOrder = 2) else it }, "internal") }
    }
    @Test fun explicitEmptyActiveFolderDoesNotExposeCachedFilesOrInventAList() {
        val nodes = folder().filterNot { it.path == "0.2.0.0" } +
            UiNode("0.2.0.1", "0.2.0", p + "empty_view", description = "no data") +
            listOf(UiNode("0.2.1", "0.2", p + "view_root", drawingOrder = 1), UiNode("0.2.1.0", "0.2.1", p + "music_lib_all_music_list")) + row(1, 0, "Cached.wav")
        val parsed = MangoUiModel.parse(nodes, "internal")
        assertTrue(parsed.rows.isEmpty()); assertNull(parsed.scroll)
        assertThrows(IllegalArgumentException::class.java) { MangoUiModel.parse(nodes.map { if (it.id == p + "empty_view") it.copy(description = "unknown") else it }, "internal") }
    }
}

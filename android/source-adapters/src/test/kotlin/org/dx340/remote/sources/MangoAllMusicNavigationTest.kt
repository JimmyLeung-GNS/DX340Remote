package org.dx340.remote.sources

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MangoAllMusicNavigationTest {
    private val prefix = "com.ibasso.music:id/"
    private val fixture = JSONObject(javaClass.getResource("/mango-navigation/all-music.json")!!.readText())
    private fun observed(): List<UiNode> = fixture.getJSONArray("nodes").let { rows ->
        (0 until rows.length()).map { i -> rows.getJSONObject(i).let { n ->
            UiNode(n.getString("path"), n.optString("parent").takeIf { !n.isNull("parent") },
                n.getString("id"), n.getString("text"), n.getString("description"), n.getString("class"),
                clickable = n.getBoolean("clickable"), forward = n.getBoolean("forward"), drawingOrder = n.getInt("drawingOrder"))
        } }
    }
    @Test fun observedAllMusicUsesOnlyItsToolbarNotCachedPlayerOrSongActions() {
        val nodes = observed()
        val action = MangoUiModel.allMusicMenuAction(nodes)!!
        assertEquals(fixture.getString("expectedArrow"), action.path)
        assertEquals(prefix + "nav_bar_back_iv", action.id)
        assertNotEquals(nodes.single { it.id == prefix + "main_list_iv" }.path, action.path)
        assertNull(MangoUiModel.playlistMenuAction(nodes))
    }
    @Test fun otherScreensDoNotAcquireAnAllMusicNavigationAction() {
        for (title in listOf("Playlist", "SD Card", "Album", "Search")) {
            assertNull(MangoUiModel.allMusicMenuAction(observed().map { if (it.id == prefix + "entry_title_tv") it.copy(text = title) else it }))
        }
        val menu = observed() + listOf(UiNode("9.0", "9", prefix + "internalStorgeLayout"), UiNode("9.1", "9", prefix + "sdcardLayout"))
        assertNull(MangoUiModel.allMusicMenuAction(menu))
    }
    @Test fun cachedPagesAndAmbiguousHeadersOrListsAreRejected() {
        val nodes = observed()
        val header = nodes.single { it.id == prefix + "entry_title_tv" }
        val list = nodes.single { it.id == prefix + "music_lib_all_music_list" }
        for (ambiguous in listOf(
            nodes + header.copy(path = "9.0", parent = "9"),
            nodes + list.copy(path = "9.1", parent = "9"),
            nodes + UiNode("9.2", "9", prefix + "folder_detail_content"),
            nodes + UiNode("9.3", "9", prefix + "playlist_view"),
            nodes + UiNode("9.4", "9", className = "android.widget.EditText"),
            nodes + nodes.first())) {
            assertThrows(RuntimeException::class.java) { MangoUiModel.allMusicMenuAction(ambiguous) }
        }
    }
    @Test fun disabledOrUnrelatedToolbarControlsAndDetachedListCannotBeClicked() {
        val nodes = observed()
        for (invalid in listOf(
            nodes.map { if (it.id == prefix + "nav_bar_back_iv") it.copy(enabled = false) else it },
            nodes.map { if (it.id == prefix + "nav_bar_back_iv") it.copy(clickable = false) else it },
            nodes.map { if (it.id == prefix + "nav_bar_back_iv") it.copy(parent = "other-bar") else it },
            nodes.map { if (it.id == prefix + "entry_title_tv") it.copy(enabled = false) else it },
            nodes.map { if (it.id == prefix + "music_lib_all_music_list") it.copy(parent = "other-page") else it })) {
            assertThrows(RuntimeException::class.java) { MangoUiModel.allMusicMenuAction(invalid) }
        }
    }
}

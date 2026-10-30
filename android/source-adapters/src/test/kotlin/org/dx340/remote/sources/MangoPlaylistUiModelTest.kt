package org.dx340.remote.sources

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class MangoPlaylistUiModelTest {
    private val p = "com.ibasso.music:id/"
    private fun fixture(name: String): List<UiNode> {
        val json = JSONArray(javaClass.getResource("/mango-playlists/$name.json")!!.readText().removePrefix("\uFEFF"))
        return (0 until json.length()).map { i ->
            val n = json.getJSONObject(i); val path = n.getString("path")
            UiNode(path, path.substringBeforeLast('.', "").takeIf { it.isNotEmpty() }, n.optString("id"), n.optString("text"),
                n.optString("description"), n.optString("class"), n.optBoolean("clickable"), forward = n.optBoolean("forward"), drawingOrder = n.optInt("drawingOrder"))
        }
    }
    @Test fun observedEmptyAndNamedPlaylistListExcludeCreationAndFooter() {
        assertTrue(MangoPlaylistUiModel.parse(fixture("empty")).rows.isEmpty())
        val parsed = MangoPlaylistUiModel.parse(fixture("list"))
        assertEquals(listOf("DX340 Remote verification"), parsed.rows.map { it.title })
        assertTrue(parsed.rows.single().browsable); assertFalse(parsed.rows.single().playable)
        assertNull(parsed.back); assertNull(parsed.root)
        assertEquals("", fixture("list").single { it.path == parsed.rows.single().path }.id)
    }
    @Test fun observedDetailOffersOnlyTwoWholeTrackRows() {
        val nodes = fixture("detail"); val parsed = MangoPlaylistUiModel.parse(nodes)
        assertEquals("DX340 Remote verification", parsed.title)
        assertEquals(listOf("白日夢", "風のたより"), parsed.rows.map { it.title })
        assertEquals(listOf("tayori", "tayori"), parsed.rows.map { it.subtitle })
        assertTrue(parsed.rows.all { it.playable && !it.browsable })
        assertTrue(parsed.rows.all { row -> nodes.single { it.path == row.path }.className == "android.widget.RelativeLayout" })
        assertNotNull(parsed.back)
    }
    @Test fun addToPlaylistChooserAndEditOrMenuScreensAreRejected() {
        for (name in listOf("chooser", "menu")) assertThrows(RuntimeException::class.java) { MangoPlaylistUiModel.parse(fixture(name)) }
        val nodes = fixture("list")
        assertThrows(RuntimeException::class.java) { MangoPlaylistUiModel.parse(nodes + UiNode("0.99", "0", className = "android.widget.EditText")) }
        assertThrows(RuntimeException::class.java) { MangoPlaylistUiModel.parse(nodes.map { if (it.id == p + "entry_title_tv") it.copy(text = "Add To Playlist") else it }) }
    }
    @Test fun titleArtistOrderCountAndContextChangesInvalidateProof() {
        val nodes = fixture("detail"); val proof = MangoPlaylistUiModel.parse(nodes).fingerprint
        for (id in listOf("item_name_tv", "music_item_title", "music_item_artist", "music_count_tv")) {
            val changed = nodes.map { if (it.id == p + id) it.copy(text = if (id == "music_count_tv") "total 3 songs" else "Changed") else it }
            assertNotEquals(id, proof, MangoPlaylistUiModel.parse(changed).fingerprint)
        }
        val swapped = nodes.map { if (it.id == p + "music_item_position") it.copy(text = if (it.text == "1") "2" else "1") else it }
        assertNotEquals(proof, MangoPlaylistUiModel.parse(swapped).fingerprint)
        assertNotEquals(proof, MangoPlaylistUiModel.parse(fixture("list")).fingerprint)
    }
    @Test fun miniPlayerChangesDoNotInvalidatePlaylistProof() {
        val nodes = fixture("detail")
        val changed = nodes.map { if (it.id == p + "music_lib_controller_title") it.copy(text = "Next track") else it }
        assertEquals(MangoPlaylistUiModel.parse(nodes).fingerprint, MangoPlaylistUiModel.parse(changed).fingerprint)
    }
    @Test fun ambiguousPositionsOrHeadersAndInnerButtonsFailClosed() {
        val nodes = fixture("detail")
        val variants = listOf(
            nodes.map { if (it.id == p + "music_item_position") it.copy(text = "1") else it },
            nodes.map { if (it.id == p + "music_item_title") it.copy(clickable = true) else it },
            nodes + nodes.single { it.id == p + "entry_title_tv" }.copy(path = "0.99"),
            nodes.map { if (it.id == p + "music_count_tv") it.copy(text = "total 1 songs") else it },
            nodes.map { if (it.id == p + "nav_bar_back_iv") it.copy(enabled = false) else it })
        variants.forEach { variant -> assertThrows(RuntimeException::class.java) { MangoPlaylistUiModel.parse(variant) } }
    }
    @Test fun menuSelectionIsSpecificAndNeverAnEditingAction() {
        val menu = fixture("menu")
        assertTrue(MangoPlaylistUiModel.menuAction(menu).id.endsWith("/allPlaylistLayout"))
        assertThrows(RuntimeException::class.java) { MangoPlaylistUiModel.menuAction(fixture("chooser")) }
        assertThrows(RuntimeException::class.java) { MangoPlaylistUiModel.menuAction(menu.map { if (it.text == "Playlist") it.copy(text = "Delete") else it }) }
    }
    @Test fun continuationUsesOnlyObservedScrollableListAndChangesProof() {
        val nodes = fixture("detail"); val parsed = MangoPlaylistUiModel.parse(nodes)
        assertNull(parsed.scroll)
        val scrolling = MangoPlaylistUiModel.parse(nodes.map { if (it.id == p + "music_lib_all_music_list") it.copy(forward = true) else it })
        assertTrue(scrolling.scroll!!.endsWith(".2")); assertNotEquals(parsed.fingerprint, scrolling.fingerprint)
        // This is a simulated continuation capability, not a physical long-playlist pass.
    }
}

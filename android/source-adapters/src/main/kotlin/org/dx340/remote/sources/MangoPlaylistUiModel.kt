package org.dx340.remote.sources

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

/** Observed Mango4.1.37/240 Playlist list/detail only. Never returns editing or overflow controls. */
object MangoPlaylistUiModel {
    const val CONTEXT = "playlists"
    private const val P = "com.ibasso.music:id/"
    private fun List<UiNode>.one(id: String) = singleOrNull { it.id == P + id } ?: error("UI_UNEXPECTED_SCREEN")
    private fun under(node: UiNode, path: String) = node.path.startsWith("$path.")
    fun menuAction(nodes: List<UiNode>): UiNode {
        require(MangoUiModel.menuVisible(nodes)) { "UI_UNEXPECTED_SCREEN" }
        val option = nodes.one("allPlaylistLayout")
        require(option.enabled && option.clickable && nodes.count { under(it, option.path) && it.text == "Playlist" } == 1) { "UI_UNEXPECTED_SCREEN" }
        return option
    }
    fun parse(nodes: List<UiNode>): ParsedMangoUi {
        require(nodes.size <= 800 && nodes.map { it.path }.distinct().size == nodes.size) { "UI_TREE_LIMIT" }
        require(!MangoUiModel.menuVisible(nodes) && nodes.none { it.className.contains("EditText") }) { "UI_UNEXPECTED_SCREEN" }
        val header = nodes.one("entry_title_tv")
        require(header.enabled && header.text == "Playlist") { "UI_UNEXPECTED_SCREEN" }
        val bar = nodes.one("nav_bar")
        require(header.parent == bar.path && bar.parent != null) { "UI_UNEXPECTED_SCREEN" }
        val back = nodes.one("nav_bar_back_iv")
        require(back.parent == bar.path && back.clickable && back.enabled) { "UI_UNEXPECTED_SCREEN" }
        val page = bar.parent!!
        val playlistList = nodes.singleOrNull { it.id == P + "playlist_view" }
        val detail = playlistList == null
        val list = playlistList ?: nodes.one("music_lib_all_music_list")
        require(under(list, page)) { "UI_UNEXPECTED_SCREEN" }
        val title: String
        val total: Int?
        if (detail) {
            require(nodes.none { it.id == P + "add_new_playlist_view" }) { "UI_UNEXPECTED_SCREEN" }
            val name = nodes.one("item_name_tv")
            val count = nodes.one("music_count_tv")
            require(under(name, page) && under(count, page)) { "UI_UNEXPECTED_SCREEN" }
            title = name.text
            total = Regex("total ([0-9]+) songs").matchEntire(count.text)?.groupValues?.get(1)?.toIntOrNull()
                ?: error("UI_UNEXPECTED_SCREEN")
        } else {
            require(list.parent == page && nodes.one("add_new_playlist_view").parent == page) { "UI_UNEXPECTED_SCREEN" }
            title = "Mango playlists"
            total = null
        }
        require(title.isNotBlank() && title.length <= 512 && '\u0000' !in title) { "UI_UNEXPECTED_SCREEN" }
        val titleId = P + if (detail) "music_item_title" else "playlist_name"
        val positionId = P + if (detail) "music_item_position" else "playlist_item_position"
        val indexed = nodes.filter { under(it, list.path) && it.id == titleId }.map { name ->
            require(name.enabled && name.text.isNotBlank() && name.text.length <= 512 && '\u0000' !in name.text) { "UI_UNEXPECTED_SCREEN" }
            val row = MangoUiModel.clickable(nodes, name, list.path) ?: error("UI_ACTION_FAILED")
            // Require the whole direct list row; an inner menu/title button is never a track action.
            require(row.parent == list.path && row.className == "android.widget.RelativeLayout") { "UI_UNEXPECTED_SCREEN" }
            val children = nodes.filter { under(it, row.path) }
            require(children.count { it.id == titleId } == 1) { "UI_UNEXPECTED_SCREEN" }
            val position = children.singleOrNull { it.id == positionId }?.text?.toIntOrNull() ?: error("UI_UNEXPECTED_SCREEN")
            require(position > 0 && (total == null || position <= total)) { "UI_UNEXPECTED_SCREEN" }
            val artist = if (detail) children.singleOrNull { it.id == P + "music_item_artist" }?.text ?: error("UI_UNEXPECTED_SCREEN") else "Existing playlist"
            position to UiRow(row.path, name.text, artist, playable = detail, browsable = !detail)
        }.sortedBy { it.first }
        require(indexed.size <= 50 && indexed.map { it.first }.distinct().size == indexed.size && indexed.map { it.second.path }.distinct().size == indexed.size) { "UI_UNEXPECTED_SCREEN" }
        require(nodes.count { it.parent == list.path } == indexed.size) { "UI_UNEXPECTED_SCREEN" }
        require(indexed.isNotEmpty() || total == null || total == 0) { "UI_UNEXPECTED_SCREEN" }
        val scroll = list.takeIf { it.forward }?.path
        // Frame semantic page identity with lengths; exclude the independent mini-player clock.
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            listOf("mango-playlist-v1", title, total.toString(), list.path, back.path, scroll.orEmpty()).forEach {
                val data = it.toByteArray(Charsets.UTF_8); out.writeInt(data.size); out.write(data)
            }
            indexed.forEach { (position, row) ->
                out.writeInt(position)
                listOf(row.path, row.title, row.subtitle).forEach { val data = it.toByteArray(Charsets.UTF_8); out.writeInt(data.size); out.write(data) }
            }
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        return ParsedMangoUi(title, CONTEXT, indexed.map { it.second }, if (detail) back.path else null, if (detail) back.path else null, scroll, hash)
    }
}

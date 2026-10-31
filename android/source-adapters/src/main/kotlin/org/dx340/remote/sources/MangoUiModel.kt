package org.dx340.remote.sources

import java.security.MessageDigest

data class ParsedMangoUi(val title: String, val storage: String, val rows: List<UiRow>, val back: String?, val root: String?, val scroll: String?, val fingerprint: String)

/** Mango4.1.37/code240 semantic folder model. Cached ancestor pages are deliberately excluded. */
object MangoUiModel {
    private const val PREFIX = "com.ibasso.music:id/"
    val storages = linkedMapOf("internal" to "Internal Storage", "sd" to "SD Card")
    fun node(nodes: List<UiNode>, id: String) = nodes.firstOrNull { it.id == PREFIX + id }
    fun clickable(nodes: List<UiNode>, value: UiNode, within: String? = null) = AppleUiModel.clickable(nodes, value, within)
    // Mango hides the SD option when no card is available; cached folder rows can remain behind this drawer.
    fun menuVisible(nodes: List<UiNode>) = node(nodes, "internalStorgeLayout") != null || node(nodes, "sdcardLayout") != null
    /** Recognize the observed Playlist page only to leave it for local-folder navigation. */
    fun playlistMenuAction(nodes: List<UiNode>): UiNode? {
        if (menuVisible(nodes)) return null
        val headers = nodes.filter { it.id == PREFIX + "entry_title_tv" }
        if (headers.none { it.text == "Playlist" }) return null
        require(headers.size == 1 && headers.single().enabled) { "UI_UNEXPECTED_SCREEN" }
        val header = headers.single()
        val list = nodes.singleOrNull { it.id == PREFIX + "playlist_view" } ?: error("UI_UNEXPECTED_SCREEN")
        val bar = nodes.singleOrNull { it.id == PREFIX + "nav_bar" && it.path == header.parent } ?: error("UI_UNEXPECTED_SCREEN")
        require(list.parent == bar.parent) { "UI_UNEXPECTED_SCREEN" }
        val arrow = nodes.singleOrNull { it.id == PREFIX + "nav_bar_back_iv" && it.parent == bar.path } ?: error("UI_UNEXPECTED_SCREEN")
        require(arrow.clickable && arrow.enabled) { "UI_ACTION_FAILED" }
        return arrow
    }
    /** Observed All Music root: its toolbar opens the library menu, never a song row. */
    fun allMusicMenuAction(nodes: List<UiNode>): UiNode? {
        if (menuVisible(nodes)) return null
        val headers = nodes.filter { it.id == PREFIX + "entry_title_tv" }
        if (headers.none { it.text == "All Music" }) return null
        require(nodes.size <= 800 && nodes.map { it.path }.distinct().size == nodes.size) { "UI_TREE_LIMIT" }
        require(headers.size == 1 && headers.single().enabled && nodes.none { it.className.contains("EditText") }) { "UI_UNEXPECTED_SCREEN" }
        require(nodes.none { it.id == PREFIX + "playlist_view" || it.id == PREFIX + "folder_detail_content" }) { "UI_ACTIVE_PAGE_UNVERIFIED" }
        val header = headers.single()
        val bar = nodes.singleOrNull { it.id == PREFIX + "nav_bar" && it.path == header.parent } ?: error("UI_UNEXPECTED_SCREEN")
        val list = nodes.singleOrNull { it.id == PREFIX + "music_lib_all_music_list" } ?: error("UI_UNEXPECTED_SCREEN")
        require(bar.parent != null && list.parent == bar.parent) { "UI_UNEXPECTED_SCREEN" }
        val arrow = nodes.singleOrNull { it.id == PREFIX + "nav_bar_back_iv" && it.parent == bar.path } ?: error("UI_UNEXPECTED_SCREEN")
        require(arrow.clickable && arrow.enabled) { "UI_ACTION_FAILED" }
        return arrow
    }
    fun storageAction(nodes: List<UiNode>, storage: String): UiNode {
        require(storage in storages && menuVisible(nodes)) { "UI_STORAGE_CONTEXT_REQUIRED" }
        val id = PREFIX + if (storage == "internal") "internalStorgeLayout" else "sdcardLayout"
        val option = nodes.singleOrNull { it.id == id } ?: error("UI_STORAGE_CONTEXT_REQUIRED")
        require(option.enabled && option.clickable) { "UI_ACTION_FAILED" }
        val expected = storages.getValue(storage)
        require(nodes.count { it.path.startsWith(option.path + ".") && it.text == expected } == 1) { "UI_UNEXPECTED_SCREEN" }
        return option
    }
    private fun ordered(nodes: List<UiNode>) = nodes.sortedWith { a, b ->
        val aa = a.path.split('.').map(String::toInt); val bb = b.path.split('.').map(String::toInt)
        var result = 0
        for (i in 0 until minOf(aa.size, bb.size)) { result = aa[i].compareTo(bb[i]); if (result != 0) break }
        if (result == 0) aa.size.compareTo(bb.size) else result
    }
    fun parse(nodes: List<UiNode>, storage: String): ParsedMangoUi {
        require(nodes.size <= 800) { "UI_TREE_LIMIT" }
        require(storage in storages && !menuVisible(nodes)) { "UI_STORAGE_CONTEXT_REQUIRED" }
        require(node(nodes, "entry_title_tv")?.text == storages.getValue(storage)) { "UI_STORAGE_CONTEXT_REQUIRED" }
        val content = nodes.singleOrNull { it.id == PREFIX + "folder_detail_content" } ?: error("UI_UNEXPECTED_SCREEN")
        val pages = ordered(nodes.filter { it.parent == content.path })
        require(pages.isNotEmpty() && pages.all { it.id == PREFIX + "view_root" }) { "UI_UNEXPECTED_SCREEN" }
        val active = pages.maxBy { it.drawingOrder }
        require(pages.size == 1 || (active.drawingOrder > 0 && pages.count { it.drawingOrder == active.drawingOrder } == 1)) { "UI_ACTIVE_PAGE_UNVERIFIED" }
        val lists = nodes.filter { it.path.startsWith(active.path + ".") && it.id == PREFIX + "music_lib_all_music_list" }
        val empty = nodes.singleOrNull { it.path.startsWith(active.path + ".") && it.id == PREFIX + "empty_view" }
        require(lists.size == 1 && empty == null || lists.isEmpty() && empty?.description == "no data") { "UI_UNEXPECTED_SCREEN" }
        val list = lists.singleOrNull()
        val names = if (list == null) emptyList() else ordered(nodes.filter { it.path.startsWith(list.path + ".") && it.id == PREFIX + "file_name_tv" })
        val rows = names.map { name ->
            require(name.text.isNotBlank() && name.text.length <= 512 && !name.text.contains('\u0000')) { "UI_UNEXPECTED_SCREEN" }
            val row = clickable(nodes, name, list!!.path) ?: error("UI_ACTION_FAILED")
            val children = nodes.filter { it.path.startsWith(row.path + ".") }
            require(children.count { it.id == PREFIX + "file_name_tv" } == 1) { "UI_UNEXPECTED_SCREEN" }
            val file = children.any { it.id == PREFIX + "file_info_index_tv" && it.text.matches(Regex("[0-9]+")) }
            val folder = children.any { it.id == PREFIX + "file_info_icon_iv" }
            require(file != folder) { "UI_UNEXPECTED_SCREEN" }
            val supported = file && (name.text.endsWith(".flac", true) || name.text.endsWith(".wav", true))
            UiRow(row.path, name.text, if (folder) "Folder" else if (supported) "Local file · Mango Player" else "Format not verified in assisted mode", supported, folder)
        }.distinctBy { it.path }
        require(rows.size <= 50) { "UI_TREE_LIMIT" }
        val crumbs = ordered(nodes.filter { it.id == PREFIX + "tv_content" })
        require(crumbs.isEmpty() || crumbs.first().text == storages.getValue(storage)) { "UI_STORAGE_CONTEXT_REQUIRED" }
        val root = crumbs.firstOrNull()?.let { clickable(nodes, it)?.path ?: error("UI_ACTION_FAILED") }
        val back = crumbs.dropLast(1).lastOrNull()?.let { clickable(nodes, it)?.path ?: error("UI_ACTION_FAILED") }
        val title = if (crumbs.isEmpty()) storages.getValue(storage) else crumbs.joinToString(" / ") { it.text }
        val scroll = list?.takeIf { it.forward }?.path
        val signature = "$storage\u0000$title\u0000${rows.joinToString("\u0000") { "${it.path}:${it.title}:${it.playable}:${it.browsable}" }}:$back:$root:$scroll"
        val hash = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        return ParsedMangoUi(title, storage, rows, back, root, scroll, hash)
    }
}

package org.dx340.remote.sources

import java.security.MessageDigest

data class UiNode(
    val path: String, val parent: String?, val id: String = "", val text: String = "", val description: String = "",
    val className: String = "", val clickable: Boolean = false, val selected: Boolean = false, val forward: Boolean = false,
    val backward: Boolean = false, val enabled: Boolean = true, val drawingOrder: Int = 0
)
data class UiRow(val path: String, val title: String, val subtitle: String = "", val playable: Boolean, val browsable: Boolean)
data class ParsedAppleUi(val title: String, val rows: List<UiRow>, val back: String?, val scroll: String?, val downloadedRoot: Boolean, val fingerprint: String)

/** Version-specific semantic model; never uses screen coordinates or private app data. */
object AppleUiModel {
    private const val PREFIX = "com.apple.android.music:id/"
    fun clickable(nodes: List<UiNode>, node: UiNode, within: String? = null): UiNode? {
        val byPath = nodes.associateBy { it.path }; var current: UiNode? = node
        repeat(20) {
            val n = current ?: return null
            if (within != null && !n.path.startsWith("$within.")) return null
            if (n.clickable && n.enabled) return n
            current = byPath[n.parent]
        }
        return null
    }
    fun libraryTab(nodes: List<UiNode>) = nodes.firstOrNull { it.id == PREFIX + "action_library" && it.description == "Library" }
        ?: nodes.firstOrNull { it.id == PREFIX + "navigation_bar_item_large_label_view" && it.text == "Library" }
    fun back(nodes: List<UiNode>) = nodes.firstOrNull { it.className == "android.widget.ImageButton" && it.description == "Navigate up" && it.clickable && it.enabled }
    fun downloaded(nodes: List<UiNode>) = nodes.firstOrNull { it.id == "library_categories-Downloaded" }
    /** Observed source error dialog; inspect only, never click its message or confirmation. */
    fun postDispatchFailure(nodes: List<UiNode>): String? {
        require(nodes.size <= 400 && nodes.map { it.path }.distinct().size == nodes.size) { "UI_TREE_LIMIT" }
        val message = nodes.singleOrNull { it.id == PREFIX + "message" && it.className == "android.widget.TextView" && it.text.isNotBlank() } ?: return null
        val parent = message.parent ?: return null
        val title = nodes.singleOrNull { it.parent == parent && it.id == PREFIX + "title" && it.className == "android.widget.TextView" && it.text.isNotBlank() }
        val confirmation = nodes.singleOrNull { it.parent == parent && it.className == "android.widget.Button" && it.clickable && it.enabled }
        return if (title != null && confirmation != null && libraryTab(nodes)?.selected != true) "UI_UNEXPECTED_SCREEN" else null
    }
    fun parse(nodes: List<UiNode>, offlineContext: Boolean): ParsedAppleUi {
        require(nodes.size <= 400) { "UI_TREE_LIMIT" }
        require(libraryTab(nodes)?.selected == true) { "UI_UNEXPECTED_SCREEN" }
        val marker = nodes.any { it.text == "Only showing music on this device." }
        val categories = nodes.filter { it.id in setOf("library_categories-Albums", "library_categories-Playlists", "library_categories-Songs", "library_categories-Artists") }
        val root = marker && categories.isNotEmpty()
        require(marker || offlineContext) { "UI_DOWNLOADED_CONTEXT_REQUIRED" }
        val container = nodes.firstOrNull { it.id == PREFIX + "library_details_list" || it.id == PREFIX + "recycler_view" }
        val header = nodes.firstOrNull { it.id == PREFIX + "collection_header_title" }?.text
            ?: nodes.firstOrNull { it.id == PREFIX + "toolbar_title" }?.text
            ?: nodes.firstOrNull { it.id == PREFIX + "main_title" }?.text
            ?: if (root) "Downloaded" else ""
        val rows = if (root) categories.mapNotNull { node -> clickable(nodes, node)?.let { UiRow(it.path, node.text, if (node.text == "Artists") "Not yet verified in assisted mode" else "", playable = false, browsable = node.text != "Artists") } }
        else {
            require(container != null && header.isNotBlank()) { "UI_UNEXPECTED_SCREEN" }
            val candidates = nodes.filter {
                if (!it.path.startsWith(container.path + ".")) false
                else if (it.id == PREFIX + "title") true
                else header == "Playlists" && container.id == PREFIX + "library_details_list" &&
                    it.id.isEmpty() && it.className == "android.widget.TextView" && it.text.isNotBlank() && it.text == it.description &&
                    nodes.any { row -> row.path == it.parent && row.parent == container.path && row.className == "android.view.ViewGroup" && row.clickable && row.enabled }
            }
            candidates.mapNotNull { title ->
                val row = clickable(nodes, title, container.path) ?: return@mapNotNull null
                val descendants = nodes.filter { it.path == row.path || it.path.startsWith(row.path + ".") }
                // The observed Songs layout uses action_button; album/playlist tracks use track_action_button.
                // A download badge must belong to this row, never just the surrounding Downloaded screen.
                val downloadedSong = descendants.any {
                    it.id in setOf(PREFIX + "track_action_button", PREFIX + "action_button") && it.description == "Downloaded Song"
                }
                val song = downloadedSong || descendants.any { it.id == PREFIX + "collection_list_item" } || row.id == PREFIX + "collection_list_item" || header == "Songs"
                if (!song && header !in setOf("Albums", "Artists", "Playlists")) return@mapNotNull null
                val subtitle = descendants.firstOrNull { it.id == PREFIX + "subtitle" }?.text ?: ""
                UiRow(row.path, title.text.take(128), subtitle.take(64), playable = song && downloadedSong, browsable = !song)
            }.distinctBy { it.path }
        }
        val scroll = (container ?: nodes.firstOrNull { it.forward && categories.any { c -> c.path.startsWith(it.path + ".") } })?.takeIf { it.forward }?.path
        val back = if (root) null else back(nodes)?.path
        val signature = header + "\u0000" + rows.joinToString("\u0000") { row ->
            // Display truncation must not make two different tracks share an action revision.
            val identity = nodes.filter { it.path == row.path || it.path.startsWith(row.path + ".") }
                .joinToString("\u0000") { "${it.path}\u0000${it.id}\u0000${it.text}\u0000${it.description}" }
            "${row.path}:${row.title}:${row.subtitle}:${row.playable}:${row.browsable}\u0000$identity"
        // A collapsing toolbar can move its Back button among root siblings while
        // the entire collection and every actionable row remain unchanged. Bind
        // navigation availability here; perform() still checks the issued action's
        // exact live path and signature before dispatch. Row paths/identity remain
        // in the fingerprint, so moving/replacing a song still expires its ID.
        } + ":back=${back != null}:$scroll"
        val hash = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        return ParsedAppleUi(header, rows, back, scroll, root, hash)
    }
}

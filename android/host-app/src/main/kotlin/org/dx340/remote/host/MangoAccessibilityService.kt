package org.dx340.remote.host

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.dx340.remote.sources.MangoUiModel
import org.dx340.remote.sources.MangoPlaylistUiModel
import org.dx340.remote.sources.ParsedMangoUi
import org.dx340.remote.sources.SourceRegistry
import org.dx340.remote.sources.UiNode
import org.dx340.remote.sources.UiSnapshotStability
import org.dx340.remote.sources.UiWindow
import org.dx340.remote.sources.UiWindowGuard
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Opt-in local-folder/existing-playlist actions for Mango4.1.37/code240. No gestures, network or file access. */
class MangoAccessibilityService : AccessibilityService() {
    companion object {
        var instance: MangoAccessibilityService? = null; private set
        const val PACKAGE = "com.ibasso.music"
        private const val PREFETCH = AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID or AccessibilityNodeInfo.FLAG_PREFETCH_UNINTERRUPTIBLE
    }
    private val main = Handler(Looper.getMainLooper())
    private val session = UUID.randomUUID().toString()
    private var sequence = 0
    private var busy = false
    private var cancelBrowse: ((String) -> Unit)? = null
    private var last: Frame? = null
    private var eventPending = false
    private var lastReadiness = "not-bound"
    private var navigationTrace = JSONObject()
    private data class Tree(val window: Int, val nodes: List<UiNode>)
    private data class Action(val path: String, val kind: String, val title: String = "", val storage: String = "", val expiresAt: Long = Long.MAX_VALUE)
    private data class Frame(val revision: String, val window: Int, val parsed: ParsedMangoUi?, val actions: Map<String, Action>, val page: Int = 0)
    override fun onServiceConnected() { instance = this; notifyHost(true) }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; cancelBrowse?.invoke("UI_PERMISSION_REQUIRED"); main.removeCallbacksAndMessages(null); last = null; notifyHost(true); return super.onUnbind(intent) }
    override fun onInterrupt() { last = null; cancelBrowse?.invoke("UI_ACTION_INTERRUPTED"); notifyHost(true) }
    fun invalidateLibrary() { last = null; cancelBrowse?.invoke("STALE_LIBRARY") }
    fun libraryDiagnostic() = JSONObject().put("hasFrame", last != null).put("busy", busy)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!eventPending) { eventPending = true; main.postDelayed({ eventPending = false; notifyHost() }, 500) }
    }
    private fun notifyHost(force: Boolean = false) {
        val current = readiness() ?: "ready"
        if (force || current != lastReadiness) { lastReadiness = current; HostService.instance?.sources?.externalStateChanged() }
    }
    fun readiness(): String? {
        if (!getSharedPreferences("mango-ui", MODE_PRIVATE).getBoolean("enabled", false)) return "UI_MODE_DISABLED"
        if (!getSystemService(PowerManager::class.java).isInteractive || getSystemService(KeyguardManager::class.java).isDeviceLocked) return "UI_SCREEN_UNLOCK_REQUIRED"
        val pi = runCatching { packageManager.getPackageInfo(PACKAGE, 0) }.getOrNull() ?: return "MISSING_SOURCE"
        if (pi.versionName != "4.1.37" || pi.longVersionCode != 240L) return "UI_VERSION_UNVERIFIED"
        return null
    }
    fun hasForegroundWindow() = rootInActiveWindow?.packageName?.toString() == PACKAGE
    private fun windowStates() = windows.map { UiWindow(it.id, it.type, it.layer, it.isActive, it.isFocused) }
    private fun requireUnobscured(window: Int) { require(UiWindowGuard.permits(window, windowStates())) { "UI_UNEXPECTED_SCREEN" } }
    fun debugWindowSnapshot(): JSONObject {
        readiness()?.let { error(it) }
        val root = rootInActiveWindow ?: error("UI_FOREGROUND_REQUIRED")
        require(root.packageName?.toString() == PACKAGE) { "UI_FOREGROUND_REQUIRED" }
        return JSONObject().put("rootWindow", root.windowId).put("flags", serviceInfo.flags).put("windows", JSONArray().apply {
            windowStates().forEach { put(JSONObject().put("id", it.id).put("type", it.type).put("layer", it.layer).put("active", it.active).put("focused", it.focused)) }
        })
    }
    private fun tree(): Tree {
        readiness()?.let { error(it) }
        // Complete the public API's bounded prefetch batch before walking it, so
        // child requests do not interrupt that batch. Live action validation stays separate.
        val root = (if (Build.VERSION.SDK_INT >= 33) getRootInActiveWindow(
            PREFETCH
        ) else rootInActiveWindow) ?: error("UI_FOREGROUND_REQUIRED")
        require(root.packageName?.toString() == PACKAGE) { "UI_FOREGROUND_REQUIRED" }
        requireUnobscured(root.windowId)
        val nodes = mutableListOf<UiNode>()
        val pending = ArrayDeque<Pair<AccessibilityNodeInfo, String>>(); pending.add(root to "0")
        while (pending.isNotEmpty()) {
            check(nodes.size < 800) { "UI_TREE_LIMIT" }
            val (node, path) = pending.removeFirst()
            if (node.packageName?.toString() != PACKAGE || node.isPassword || node.isEditable || !node.isVisibleToUser) continue
            val actions = node.actionList.map { it.id }
            nodes.add(UiNode(path, path.substringBeforeLast('.', "").takeIf { it.isNotEmpty() }, node.viewIdResourceName ?: "",
                node.text?.toString()?.take(512) ?: "", node.contentDescription?.toString()?.take(256) ?: "", node.className?.toString() ?: "",
                node.isClickable, node.isSelected, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD in actions,
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD in actions, node.isEnabled, node.drawingOrder))
            require(path.count { it == '.' } <= 32 && node.childCount <= 100) { "UI_TREE_LIMIT" }
            val count = node.childCount
            for (i in 0 until count) {
                val child = try { if (Build.VERSION.SDK_INT >= 33) node.getChild(i, PREFETCH) else node.getChild(i) } catch (_: IndexOutOfBoundsException) { error("UI_TREE_CHANGED") }
                child?.let { pending.add(it to "$path.$i") }
            }
        }
        return Tree(root.windowId, nodes)
    }
    fun debugSnapshot(): JSONObject {
        val current = tree()
        return JSONObject().put("window", current.window).put("navigation", navigationTrace).put("nodes", JSONArray().apply { current.nodes.forEach { n ->
            put(JSONObject().put("path", n.path).put("id", n.id).put("text", n.text).put("description", n.description)
                .put("class", n.className).put("clickable", n.clickable).put("forward", n.forward).put("drawingOrder", n.drawingOrder))
        } })
    }
    private fun home(): JSONObject {
        val revision = "$session:home:${++sequence}"
        val actions = MangoUiModel.storages.map { (key, title) -> "mango-ui:$session:$sequence:$key" to Action("", "storage", title, key) }.toMap()
        last = Frame(revision, -1, null, actions)
        return JSONObject().put("uiAssisted", true).put("title", "Mango local folders").put("revision", revision).put("page", 0)
            .put("parent", SourceRegistry.MANGO_UI_ROOT).put("backId", SourceRegistry.MANGO_NATIVE_ROOT).put("nextPage", JSONObject.NULL)
            .put("items", JSONArray().apply { actions.forEach { (id, action) -> put(JSONObject().put("id", id).put("title", action.title)
                .put("subtitle", "DX340 must stay awake and unlocked").put("browsable", true).put("playable", false)) } })
    }
    private fun result(current: Tree, storage: String, page: Int, end: Boolean = false): JSONObject {
        val parsed = parse(current.nodes, storage)
        val revision = "$session:${current.window}:${++sequence}:${parsed.fingerprint}"
        val actions = linkedMapOf<String, Action>()
        parsed.rows.forEachIndexed { i, row -> actions["mango-ui:$session:$sequence:$i"] = Action(row.path, if (row.playable) "play" else if (row.browsable) "folder" else "disabled", row.title, storage) }
        fun labelAt(path: String) = current.nodes.firstOrNull { it.path.startsWith("$path.") && it.id.endsWith("/tv_content") }?.text ?: ""
        val backId = "mango-ui:$session:$sequence:back"
        actions[backId] = parsed.back?.let { Action(it, "back", labelAt(it), storage) } ?: Action("", "home")
        val scrollId = parsed.scroll?.let { "mango-ui:$session:$sequence:scroll" }
        if (scrollId != null) actions[scrollId] = Action(parsed.scroll!!, "scroll", storage = storage)
        last = Frame(revision, current.window, parsed, actions, page)
        return JSONObject().put("uiAssisted", true).put("title", parsed.title).put("revision", revision).put("page", page)
            .put("parent", scrollId ?: if (storage == MangoPlaylistUiModel.CONTEXT) SourceRegistry.MANGO_PLAYLIST_UI_ROOT else SourceRegistry.MANGO_UI_ROOT)
            .put("backId", if (storage == MangoPlaylistUiModel.CONTEXT && parsed.back == null) SourceRegistry.MANGO_NATIVE_ROOT else backId)
            .put("nextPage", if (!end && scrollId != null) page + 1 else JSONObject.NULL)
            .put("items", JSONArray().apply { if (!end) parsed.rows.forEachIndexed { i, row -> put(JSONObject().put("id", "mango-ui:$session:$sequence:$i")
                .put("title", row.title).put("subtitle", row.subtitle).put("playable", row.playable).put("browsable", row.browsable)) } })
    }
    private fun parse(nodes: List<UiNode>, storage: String) = if (storage == MangoPlaylistUiModel.CONTEXT) MangoPlaylistUiModel.parse(nodes) else MangoUiModel.parse(nodes, storage)
    private fun validate(item: String, revision: String, kind: String? = null): Pair<Tree?, Action> {
        readiness()?.let { error(it) }
        val previous = last ?: error("STALE_LIBRARY")
        require(previous.revision == revision) { "STALE_LIBRARY" }
        val action = previous.actions[item] ?: error("EXPIRED_ITEM")
        require(kind == null || action.kind == kind) { "UNSUPPORTED_CAPABILITY" }
        if (previous.parsed == null) { require(action.kind == "storage") { "UNSUPPORTED_CAPABILITY" }; return null to action }
        val current = tree(); val parsed = parse(current.nodes, previous.parsed.storage)
        require(current.window == previous.window && parsed.fingerprint == previous.parsed.fingerprint) { "STALE_LIBRARY" }
        return current to action
    }
    private fun perform(current: Tree, action: Action, allowed: () -> Boolean) {
        require(android.os.SystemClock.elapsedRealtime() <= action.expiresAt) { "UI_ACTION_TIMEOUT" }
        require(allowed()) { "AUTHORIZATION_DENIED" }; readiness()?.let { error(it) }
        requireUnobscured(current.window)
        val expected = current.nodes.firstOrNull { it.path == action.path } ?: error("STALE_LIBRARY")
        var live = rootInActiveWindow ?: error("UI_FOREGROUND_REQUIRED")
        require(live.packageName?.toString() == PACKAGE && live.windowId == current.window) { "STALE_LIBRARY" }
        for (index in action.path.split('.').drop(1)) live = live.getChild(index.toInt()) ?: error("STALE_LIBRARY")
        require(live.refresh() && live.isVisibleToUser && live.isEnabled && live.viewIdResourceName.orEmpty() == expected.id && live.className?.toString().orEmpty() == expected.className) { "STALE_LIBRARY" }
        if (action.title.isNotEmpty()) {
            val descendants = ArrayDeque<AccessibilityNodeInfo>(); descendants.add(live)
            val labels = mutableListOf<String>(); var count = 0
            while (descendants.isNotEmpty()) {
                require(count++ < 60) { "UI_TREE_LIMIT" }; val node = descendants.removeFirst()
                if (node.isPassword || node.isEditable || !node.isVisibleToUser) continue
                if (node.viewIdResourceName?.substringAfterLast('/') in setOf("file_name_tv", "tv_content", "playlist_name", "music_item_title") || action.kind == "storage") node.text?.toString()?.takeIf { it.isNotBlank() }?.let(labels::add)
                require(node.childCount <= 30) { "UI_TREE_LIMIT" }
                for (i in 0 until node.childCount) node.getChild(i)?.let(descendants::add)
            }
            require(labels.count { it == action.title } == 1) { "STALE_LIBRARY" }
        }
        require(allowed()) { "AUTHORIZATION_DENIED" }
        requireUnobscured(current.window)
        require(android.os.SystemClock.elapsedRealtime() <= action.expiresAt) { "UI_ACTION_TIMEOUT" }
        check(live.performAction(if (action.kind == "scroll") AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_CLICK)) { "UI_ACTION_FAILED" }
    }
    fun validatePlayable(item: String, revision: String): String? = try { validate(item, revision, "play"); null } catch (e: Exception) { e.message ?: "UI_UNEXPECTED_SCREEN" }
    /** Reload folder listings through Mango before file dispatch; its visible list can be cached after USB changes. */
    private fun prepareFreshPlay(item: String, revision: String, allowed: () -> Boolean, done: (Tree?, Action?, String?) -> Unit) {
        if (busy) { done(null, null, "UI_BUSY"); return }
        val deadline = android.os.SystemClock.elapsedRealtime() + 20000
        val guarded = { allowed() && android.os.SystemClock.elapsedRealtime() <= deadline }
        fun fail(issue: String) { last = null; done(null, null, if (android.os.SystemClock.elapsedRealtime() > deadline) "UI_ACTION_TIMEOUT" else issue) }
        try {
            require(guarded()) { "AUTHORIZATION_DENIED" }
            val (current, original) = validate(item, revision, "play")
            val frame = last!!; val parsed = frame.parsed!!
            if (parsed.storage == MangoPlaylistUiModel.CONTEXT) { done(current, original, null); return }
            fun checked(value: JSONObject?, issue: String?) {
                if (issue != null || value == null) { fail(issue ?: "UI_UNEXPECTED_SCREEN"); return }
                try {
                    require(guarded()) { "AUTHORIZATION_DENIED" }
                    val fresh = last ?: error("STALE_LIBRARY")
                    require(fresh.window == frame.window) { "STALE_LIBRARY" }
                    // Mango can retain its scroll offset when reopening. Trust only an
                    // exact refreshed viewport proof, not the newly assigned logical page.
                    if (fresh.parsed?.fingerprint != parsed.fingerprint) {
                        require(value.getInt("page") < frame.page && !value.isNull("nextPage")) { "STALE_LIBRARY" }
                        browse(value.getString("parent"), value.getInt("nextPage"), last!!.revision, guarded, ::checked)
                        return
                    }
                    val matches = fresh.actions.filterValues { it.kind == "play" && it.title == original.title }
                    require(matches.size == 1) { "STALE_LIBRARY" }
                    val (tree, action) = validate(matches.keys.single(), fresh.revision, "play")
                    require(guarded()) { "AUTHORIZATION_DENIED" }
                    done(tree, action.copy(expiresAt = deadline), null)
                } catch (e: Exception) { fail(e.message ?: "UI_UNEXPECTED_SCREEN") }
            }
            if (parsed.back == null) {
                // A storage-root refresh needs leaving/re-entering that storage via the observed Music drawer.
                val arrow = MangoUiModel.node(current!!.nodes, "nav_bar_back_iv") ?: error("UI_UNEXPECTED_SCREEN")
                val menu = MangoUiModel.clickable(current.nodes, arrow) ?: error("UI_ACTION_FAILED")
                perform(current, Action(menu.path, "menu"), guarded)
                val choices = home()
                val storage = last!!.actions.entries.single { it.value.kind == "storage" && it.value.storage == parsed.storage }
                browse(storage.key, 0, choices.getString("revision"), guarded, ::checked)
            } else {
                val parentTitle = parsed.title.substringBeforeLast(" / ", "")
                val folderTitle = parsed.title.substringAfterLast(" / ")
                val back = frame.actions.entries.single { it.value.kind == "back" }
                fun openFolder(parent: JSONObject?, issue: String?) {
                    if (issue != null || parent == null) { fail(issue ?: "UI_UNEXPECTED_SCREEN"); return }
                    try {
                        require(guarded()) { "AUTHORIZATION_DENIED" }
                        require(last?.parsed?.title == parentTitle && last?.parsed?.storage == parsed.storage) { "STALE_LIBRARY" }
                        val folders = last!!.actions.filterValues { it.kind == "folder" && it.title == folderTitle }
                        require(folders.size <= 1) { "STALE_LIBRARY" }
                        if (folders.isNotEmpty()) browse(folders.keys.single(), 0, last!!.revision, guarded, ::checked)
                        else {
                            require(!parent.isNull("nextPage")) { "STALE_LIBRARY" }
                            browse(parent.getString("parent"), parent.getInt("nextPage"), last!!.revision, guarded, ::openFolder)
                        }
                    } catch (e: Exception) { fail(e.message ?: "UI_UNEXPECTED_SCREEN") }
                }
                browse(back.key, 0, revision, guarded, ::openFolder)
            }
        } catch (e: Exception) { fail(e.message ?: "UI_UNEXPECTED_SCREEN") }
    }
    fun validateFreshPlayable(item: String, revision: String, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        prepareFreshPlay(item, revision, stillAuthorized) { _, _, issue -> done(issue) }
    }
    fun play(item: String, revision: String, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        prepareFreshPlay(item, revision, stillAuthorized) { current, action, issue ->
            if (issue != null) done(issue)
            else try { perform(current!!, action!!, stillAuthorized); last = null; done(null) }
            catch (e: Exception) { done(e.message ?: "UI_ACTION_FAILED") }
        }
    }
    fun browse(parent: String?, page: Int, revision: String?, stillAuthorized: () -> Boolean, done: (JSONObject?, String?) -> Unit) {
        if (busy) { done(null, "UI_BUSY"); return }; busy = true
        val startedAt = android.os.SystemClock.elapsedRealtime()
        val steps = JSONArray()
        navigationTrace = JSONObject().put("steps", steps)
        fun trace(kind: String, details: JSONObject = JSONObject()) {
            if (steps.length() < 64) steps.put(details.put("kind", kind).put("elapsedMs", android.os.SystemClock.elapsedRealtime() - startedAt))
        }
        var finished = false
        fun finish(value: JSONObject?, error: String?) { if (!finished) { finished = true; busy = false; cancelBrowse = null; if (error != null) last = null; trace("finished", JSONObject().put("error", error ?: JSONObject.NULL)); done(value, error) } }
        cancelBrowse = { finish(null, it) }
        val deadline = android.os.SystemClock.elapsedRealtime() + 8000
        fun guard() { require(stillAuthorized()) { "AUTHORIZATION_DENIED" }; readiness()?.let { error(it) }; check(android.os.SystemClock.elapsedRealtime() <= deadline) { "UI_ACTION_TIMEOUT" } }
        var storageClicked = false; var menuClicked = false; var libraryClicked = false; var resetFingerprint = ""
        var reloadReturnStorage: String? = null
        val foregroundStartup = org.dx340.remote.sources.UiForegroundStartup()
        var settled = ""; var since = 0L
        var navigationStability = UiSnapshotStability()
        fun openStorage(storage: String) {
            if (finished) return
            try {
                guard(); val treeStarted = android.os.SystemClock.elapsedRealtime(); val current = tree()
                foregroundStartup.observed()
                val treeMs = android.os.SystemClock.elapsedRealtime() - treeStarted
                // Mango changes child indices during page animations. Only navigate from a
                // stable semantic tree; a successful action is still dispatched just once.
                val shownStorage = MangoUiModel.storages.entries.firstOrNull {
                    MangoUiModel.node(current.nodes, "entry_title_tv")?.text == it.value
                }?.key ?: storage
                val parsed = runCatching { MangoUiModel.parse(current.nodes, shownStorage) }.getOrNull()
                val playlist = runCatching { MangoPlaylistUiModel.parse(current.nodes) }.getOrNull()
                val menu = MangoUiModel.menuVisible(current.nodes)
                // Playback time text must not prevent a folder from settling.
                val signature = playlist?.fingerprint ?: parsed?.fingerprint ?: current.nodes.filter {
                    it.id.endsWith("/internalStorgeLayout") || it.id.endsWith("/sdcardLayout") || it.id.endsWith("/main_list_iv") || it.id.endsWith("/nav_bar_back_iv") ||
                        it.id.endsWith("/entry_title_tv") || it.id.endsWith("/playlist_view")
                }.joinToString(";") { "${it.path}:${it.id}:${it.text}" }
                val now = android.os.SystemClock.elapsedRealtime()
                val stableMs = navigationStability.observe(current.window, signature, now)
                navigationTrace.put("storage", storage).put("menu", menu).put("storageClicked", storageClicked)
                    .put("title", parsed?.title).put("root", parsed?.root).put("resetFingerprint", resetFingerprint)
                    .put("fingerprint", parsed?.fingerprint).put("stableMs", stableMs).put("remainingMs", deadline - now)
                trace("observed", JSONObject().put("treeMs", treeMs).put("nodes", current.nodes.size).put("menu", menu)
                    .put("title", parsed?.title ?: JSONObject.NULL).put("stableMs", stableMs))
                // One complete 450ms settling interval for a final folder. Previously the
                // 300ms navigation wait was followed by a separate 450ms wait on that same
                // fingerprint. Intermediate menus still settle for 300ms before any action.
                val finalFolder = parsed?.storage == storage && parsed.root == null || storage == MangoPlaylistUiModel.CONTEXT && playlist?.root == null && playlist != null
                if (stableMs < if (finalFolder) 450 else 300) { main.postDelayed({ openStorage(storage) }, 180); return }
                // Mango retains an obsolete SD directory list even when its SD menu
                // entry is selected again. A deliberate root browse leaves that storage
                // once before returning; it never retries the old folder/file action.
                if (storage == "sd" && parsed?.storage == "sd" && parsed.root == null &&
                    !storageClicked && !menuClicked && reloadReturnStorage == null) {
                    reloadReturnStorage = storage
                    trace("sd-reload-via-internal")
                    openStorage("internal"); return
                }
                if (menu) {
                    if (!storageClicked) {
                        val node = if (storage == MangoPlaylistUiModel.CONTEXT) MangoPlaylistUiModel.menuAction(current.nodes) else MangoUiModel.storageAction(current.nodes, storage)
                        perform(current, Action(node.path, "storage", if (storage == MangoPlaylistUiModel.CONTEXT) "Playlist" else MangoUiModel.storages.getValue(storage)), stillAuthorized)
                        storageClicked = true; trace("storage-click")
                    }
                } else {
                    if (playlist != null) {
                        val playlistBack = playlist.root
                        if (playlistBack != null) {
                            if (resetFingerprint != playlist.fingerprint) {
                                perform(current, Action(playlistBack, "back"), stillAuthorized); resetFingerprint = playlist.fingerprint
                            }
                        } else if (storage == MangoPlaylistUiModel.CONTEXT) {
                            guard(); finish(result(current, storage, 0), null); return
                        } else if (!menuClicked) {
                            val node = MangoUiModel.playlistMenuAction(current.nodes) ?: error("UI_UNEXPECTED_SCREEN")
                            perform(current, Action(node.path, "menu"), stillAuthorized); menuClicked = true
                        }
                    } else if (parsed != null) {
                        val rootPath = parsed.root
                        if (rootPath != null) {
                            // Mango may pop only one cached page even for the storage crumb.
                            // Continue only after a newly observed, settled folder snapshot.
                            if (resetFingerprint != parsed.fingerprint) {
                                perform(current, Action(rootPath, "back", MangoUiModel.storages.getValue(parsed.storage)), stillAuthorized)
                                resetFingerprint = parsed.fingerprint
                            }
                        }
                        else if (parsed.storage == storage) {
                            val returnStorage = reloadReturnStorage
                            if (returnStorage != null) {
                                reloadReturnStorage = null
                                storageClicked = false; menuClicked = false; libraryClicked = false
                                resetFingerprint = ""; navigationStability = UiSnapshotStability()
                                trace("sd-reload-return")
                                main.postDelayed({ openStorage(returnStorage) }, 180); return
                            }
                            guard(); finish(result(current, storage, 0), null); return
                        } else if (!menuClicked) {
                            val node = MangoUiModel.node(current.nodes, "nav_bar_back_iv") ?: error("UI_UNEXPECTED_SCREEN")
                            val click = MangoUiModel.clickable(current.nodes, node) ?: error("UI_ACTION_FAILED")
                            perform(current, Action(click.path, "menu"), stillAuthorized); menuClicked = true
                        }
                    } else if (!storageClicked && !menuClicked && MangoUiModel.playlistMenuAction(current.nodes) != null) {
                        val node = MangoUiModel.playlistMenuAction(current.nodes)!!
                        perform(current, Action(node.path, "menu"), stillAuthorized); menuClicked = true; trace("playlist-menu-click")
                    } else if (!storageClicked && !menuClicked && MangoUiModel.allMusicMenuAction(current.nodes) != null) {
                        val node = MangoUiModel.allMusicMenuAction(current.nodes)!!
                        perform(current, Action(node.path, "menu"), stillAuthorized); menuClicked = true; trace("all-music-menu-click")
                    } else if (!storageClicked && MangoUiModel.node(current.nodes, "folder_detail_content") != null) {
                        if (!menuClicked) { val node = MangoUiModel.node(current.nodes, "nav_bar_back_iv") ?: error("UI_UNEXPECTED_SCREEN"); val click = MangoUiModel.clickable(current.nodes, node) ?: error("UI_ACTION_FAILED"); perform(current, Action(click.path, "menu"), stillAuthorized); menuClicked = true }
                    } else if (!storageClicked && !libraryClicked) {
                        val node = MangoUiModel.node(current.nodes, "main_list_iv")
                        if (node != null) { val click = MangoUiModel.clickable(current.nodes, node) ?: error("UI_ACTION_FAILED"); perform(current, Action(click.path, "library"), stillAuthorized); libraryClicked = true }
                    }
                }
                main.postDelayed({ openStorage(storage) }, 180)
            } catch (e: Exception) {
                if (e.message == "UI_FOREGROUND_REQUIRED") {
                    try {
                        guard()
                        when (foregroundStartup.missing(android.os.SystemClock.elapsedRealtime())) {
                            org.dx340.remote.sources.UiForegroundStartup.Missing.LAUNCH -> {
                                val launch = packageManager.getLaunchIntentForPackage(PACKAGE) ?: error("MISSING_SOURCE")
                                startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); trace("foreground-launch")
                                main.postDelayed({ openStorage(storage) }, 200)
                            }
                            org.dx340.remote.sources.UiForegroundStartup.Missing.WAIT -> {
                                trace("foreground-wait"); main.postDelayed({ openStorage(storage) }, 180)
                            }
                            org.dx340.remote.sources.UiForegroundStartup.Missing.FAIL -> finish(null, "UI_FOREGROUND_REQUIRED")
                        }
                    }
                    catch (failure: Exception) { finish(null, failure.message ?: "UI_SETUP_REQUIRED") }
                } else if (e.message == "UI_TREE_CHANGED" && android.os.SystemClock.elapsedRealtime() < deadline) {
                    navigationStability = UiSnapshotStability(); main.postDelayed({ openStorage(storage) }, 180)
                } else finish(null, e.message ?: "UI_UNEXPECTED_SCREEN")
            }
        }
        fun waitChanged(storage: String, before: String, endAllowed: Boolean) {
            if (finished) return
            try {
                guard(); val current = tree(); val parsed = parse(current.nodes, storage)
                if (parsed.fingerprint != before) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (settled != parsed.fingerprint) { settled = parsed.fingerprint; since = now }
                    else if (now - since >= 450) { guard(); finish(result(current, storage, page), null); return }
                } else if (endAllowed && android.os.SystemClock.elapsedRealtime() + 450 >= deadline) { finish(result(current, storage, page, true), null); return }
                else { settled = "" }
                main.postDelayed({ waitChanged(storage, before, endAllowed) }, 150)
            } catch (e: Exception) {
                if (e.message in setOf("UI_TREE_CHANGED", "UI_UNEXPECTED_SCREEN", "UI_ACTIVE_PAGE_UNVERIFIED") && android.os.SystemClock.elapsedRealtime() < deadline) {
                    settled = ""; main.postDelayed({ waitChanged(storage, before, endAllowed) }, 150)
                } else finish(null, e.message ?: "UI_UNEXPECTED_SCREEN")
            }
        }
        try {
            guard()
            if (parent == null) { finish(home(), null); return }
            if (parent == SourceRegistry.MANGO_PLAYLIST_UI_ROOT) { require(page == 0) { "INVALID_ARGUMENT" }; openStorage(MangoPlaylistUiModel.CONTEXT); return }
            val (current, action) = validate(parent, revision ?: "")
            when (action.kind) {
                "storage" -> openStorage(action.storage)
                "home" -> finish(home(), null)
                "folder", "back", "scroll" -> { val before = last!!.parsed!!.fingerprint; perform(current!!, action, stillAuthorized); main.postDelayed({ waitChanged(action.storage, before, action.kind == "scroll") }, 180) }
                else -> finish(null, "UNSUPPORTED_CAPABILITY")
            }
        } catch (e: Exception) { finish(null, e.message ?: "UI_UNEXPECTED_SCREEN") }
    }
}

package org.dx340.remote.host

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.dx340.remote.sources.MangoUiModel
import org.dx340.remote.sources.ParsedMangoUi
import org.dx340.remote.sources.SourceRegistry
import org.dx340.remote.sources.UiNode
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Opt-in local-folder actions for Mango4.1.37/code240. No gestures, network or file access. */
class MangoAccessibilityService : AccessibilityService() {
    companion object {
        var instance: MangoAccessibilityService? = null; private set
        const val PACKAGE = "com.ibasso.music"
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
    private data class Action(val path: String, val kind: String, val title: String = "", val storage: String = "")
    private data class Frame(val revision: String, val window: Int, val parsed: ParsedMangoUi?, val actions: Map<String, Action>)
    override fun onServiceConnected() { instance = this; notifyHost(true) }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; cancelBrowse?.invoke("UI_PERMISSION_REQUIRED"); main.removeCallbacksAndMessages(null); last = null; notifyHost(true); return super.onUnbind(intent) }
    override fun onInterrupt() { last = null; cancelBrowse?.invoke("UI_ACTION_INTERRUPTED"); notifyHost(true) }
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
    private fun tree(): Tree {
        readiness()?.let { error(it) }
        val root = rootInActiveWindow ?: error("UI_FOREGROUND_REQUIRED")
        require(root.packageName?.toString() == PACKAGE) { "UI_FOREGROUND_REQUIRED" }
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
                val child = try { node.getChild(i) } catch (_: IndexOutOfBoundsException) { error("UI_TREE_CHANGED") }
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
        val parsed = MangoUiModel.parse(current.nodes, storage)
        val revision = "$session:${current.window}:${++sequence}:${parsed.fingerprint}"
        val actions = linkedMapOf<String, Action>()
        parsed.rows.forEachIndexed { i, row -> actions["mango-ui:$session:$sequence:$i"] = Action(row.path, if (row.playable) "play" else if (row.browsable) "folder" else "disabled", row.title, storage) }
        fun labelAt(path: String) = current.nodes.firstOrNull { it.path.startsWith("$path.") && it.id.endsWith("/tv_content") }?.text ?: ""
        val backId = "mango-ui:$session:$sequence:back"
        actions[backId] = parsed.back?.let { Action(it, "back", labelAt(it), storage) } ?: Action("", "home")
        val scrollId = parsed.scroll?.let { "mango-ui:$session:$sequence:scroll" }
        if (scrollId != null) actions[scrollId] = Action(parsed.scroll!!, "scroll", storage = storage)
        last = Frame(revision, current.window, parsed, actions)
        return JSONObject().put("uiAssisted", true).put("title", parsed.title).put("revision", revision).put("page", page)
            .put("parent", scrollId ?: SourceRegistry.MANGO_UI_ROOT).put("backId", backId).put("nextPage", if (!end && scrollId != null) page + 1 else JSONObject.NULL)
            .put("items", JSONArray().apply { if (!end) parsed.rows.forEachIndexed { i, row -> put(JSONObject().put("id", "mango-ui:$session:$sequence:$i")
                .put("title", row.title).put("subtitle", row.subtitle).put("playable", row.playable).put("browsable", row.browsable)) } })
    }
    private fun validate(item: String, revision: String, kind: String? = null): Pair<Tree?, Action> {
        readiness()?.let { error(it) }
        val previous = last ?: error("STALE_LIBRARY")
        require(previous.revision == revision) { "STALE_LIBRARY" }
        val action = previous.actions[item] ?: error("EXPIRED_ITEM")
        require(kind == null || action.kind == kind) { "UNSUPPORTED_CAPABILITY" }
        if (previous.parsed == null) { require(action.kind == "storage") { "UNSUPPORTED_CAPABILITY" }; return null to action }
        val current = tree(); val parsed = MangoUiModel.parse(current.nodes, previous.parsed.storage)
        require(current.window == previous.window && parsed.fingerprint == previous.parsed.fingerprint) { "STALE_LIBRARY" }
        return current to action
    }
    private fun perform(current: Tree, action: Action, allowed: () -> Boolean) {
        require(allowed()) { "AUTHORIZATION_DENIED" }; readiness()?.let { error(it) }
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
                if (node.viewIdResourceName?.endsWith("/file_name_tv") == true || node.viewIdResourceName?.endsWith("/tv_content") == true || action.kind == "storage") node.text?.toString()?.takeIf { it.isNotBlank() }?.let(labels::add)
                require(node.childCount <= 30) { "UI_TREE_LIMIT" }
                for (i in 0 until node.childCount) node.getChild(i)?.let(descendants::add)
            }
            require(labels.count { it == action.title } == 1) { "STALE_LIBRARY" }
        }
        require(allowed()) { "AUTHORIZATION_DENIED" }
        check(live.performAction(if (action.kind == "scroll") AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_CLICK)) { "UI_ACTION_FAILED" }
    }
    fun validatePlayable(item: String, revision: String): String? = try { validate(item, revision, "play"); null } catch (e: Exception) { e.message ?: "UI_UNEXPECTED_SCREEN" }
    fun play(item: String, revision: String, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        if (busy) { done("UI_BUSY"); return }
        try { val (current, action) = validate(item, revision, "play"); perform(current!!, action, stillAuthorized); last = null; done(null) }
        catch (e: Exception) { done(e.message ?: "UI_ACTION_FAILED") }
    }
    fun browse(parent: String?, page: Int, revision: String?, stillAuthorized: () -> Boolean, done: (JSONObject?, String?) -> Unit) {
        if (busy) { done(null, "UI_BUSY"); return }; busy = true
        var finished = false
        fun finish(value: JSONObject?, error: String?) { if (!finished) { finished = true; busy = false; cancelBrowse = null; done(value, error) } }
        cancelBrowse = { finish(null, it) }
        val deadline = android.os.SystemClock.elapsedRealtime() + 8000
        fun guard() { require(stillAuthorized()) { "AUTHORIZATION_DENIED" }; readiness()?.let { error(it) }; check(android.os.SystemClock.elapsedRealtime() <= deadline) { "UI_ACTION_TIMEOUT" } }
        var storageClicked = false; var menuClicked = false; var libraryClicked = false; var resetFingerprint = ""; var launched = false
        var settled = ""; var since = 0L
        var navigationTree = ""; var navigationSince = 0L
        fun openStorage(storage: String) {
            if (finished) return
            try {
                guard(); val current = tree()
                // Mango changes child indices during page animations. Only navigate from a
                // stable semantic tree; a successful action is still dispatched just once.
                val shownStorage = MangoUiModel.storages.entries.firstOrNull {
                    MangoUiModel.node(current.nodes, "entry_title_tv")?.text == it.value
                }?.key ?: storage
                val parsed = runCatching { MangoUiModel.parse(current.nodes, shownStorage) }.getOrNull()
                val menu = MangoUiModel.menuVisible(current.nodes)
                // Playback time text must not prevent a folder from settling.
                val signature = parsed?.fingerprint ?: current.nodes.filter {
                    it.id.endsWith("/internalStorgeLayout") || it.id.endsWith("/sdcardLayout") || it.id.endsWith("/main_list_iv") || it.id.endsWith("/nav_bar_back_iv")
                }.joinToString(";") { "${it.path}:${it.id}" }
                val now = android.os.SystemClock.elapsedRealtime()
                navigationTrace = JSONObject().put("storage", storage).put("menu", menu).put("storageClicked", storageClicked)
                    .put("title", parsed?.title).put("root", parsed?.root).put("resetFingerprint", resetFingerprint)
                    .put("fingerprint", parsed?.fingerprint).put("stableMs", now - navigationSince).put("remainingMs", deadline - now)
                if (navigationTree != signature) { navigationTree = signature; navigationSince = now }
                if (now - navigationSince < 300) { main.postDelayed({ openStorage(storage) }, 180); return }
                if (menu) {
                    if (!storageClicked) { val node = MangoUiModel.storageAction(current.nodes, storage); perform(current, Action(node.path, "storage", MangoUiModel.storages.getValue(storage)), stillAuthorized); storageClicked = true }
                } else {
                    if (parsed != null) {
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
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (settled != parsed.fingerprint) { settled = parsed.fingerprint; since = now }
                            else if (now - since >= 450) { guard(); finish(result(current, storage, 0), null); return }
                        } else if (!menuClicked) {
                            val node = MangoUiModel.node(current.nodes, "nav_bar_back_iv") ?: error("UI_UNEXPECTED_SCREEN")
                            val click = MangoUiModel.clickable(current.nodes, node) ?: error("UI_ACTION_FAILED")
                            perform(current, Action(click.path, "menu"), stillAuthorized); menuClicked = true
                        }
                    } else if (!storageClicked && MangoUiModel.node(current.nodes, "folder_detail_content") != null) {
                        if (!menuClicked) { val node = MangoUiModel.node(current.nodes, "nav_bar_back_iv") ?: error("UI_UNEXPECTED_SCREEN"); val click = MangoUiModel.clickable(current.nodes, node) ?: error("UI_ACTION_FAILED"); perform(current, Action(click.path, "menu"), stillAuthorized); menuClicked = true }
                    } else if (!storageClicked && !libraryClicked) {
                        val node = MangoUiModel.node(current.nodes, "main_list_iv")
                        if (node != null) { val click = MangoUiModel.clickable(current.nodes, node) ?: error("UI_ACTION_FAILED"); perform(current, Action(click.path, "library"), stillAuthorized); libraryClicked = true }
                    }
                }
                main.postDelayed({ openStorage(storage) }, 180)
            } catch (e: Exception) {
                if (e.message == "UI_FOREGROUND_REQUIRED" && !launched) {
                    try { guard(); val launch = packageManager.getLaunchIntentForPackage(PACKAGE) ?: error("MISSING_SOURCE"); startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); launched = true; main.postDelayed({ openStorage(storage) }, 200) }
                    catch (failure: Exception) { finish(null, failure.message ?: "UI_SETUP_REQUIRED") }
                } else if (e.message == "UI_TREE_CHANGED" && android.os.SystemClock.elapsedRealtime() < deadline) {
                    navigationTree = ""; main.postDelayed({ openStorage(storage) }, 180)
                } else finish(null, e.message ?: "UI_UNEXPECTED_SCREEN")
            }
        }
        fun waitChanged(storage: String, before: String, endAllowed: Boolean) {
            if (finished) return
            try {
                guard(); val current = tree(); val parsed = MangoUiModel.parse(current.nodes, storage)
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

package org.dx340.remote.host

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.dx340.remote.sources.AppleUiModel
import org.dx340.remote.sources.ParsedAppleUi
import org.dx340.remote.sources.UiNode
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Opt-in Apple-library-only node actions. No gestures, coordinates, credentials or global actions. */
class AppleMusicAccessibilityService : AccessibilityService() {
    companion object {
        var instance: AppleMusicAccessibilityService? = null; private set
        const val PACKAGE = "com.apple.android.music"
    }
    private val main = Handler(Looper.getMainLooper())
    private val session = UUID.randomUUID().toString()
    private var offlineContext = false
    private var busy = false
    private var cancelBrowse: ((String) -> Unit)? = null
    private var last: Frame? = null
    private var eventPending = false
    private var lastReadiness = "not-bound"
    private var lastValidation: JSONObject? = null
    private data class Tree(val window: Int, val nodes: List<UiNode>)
    private data class Action(val path: String, val kind: String, val signature: String)
    private data class Frame(val revision: String, val window: Int, val parsed: ParsedAppleUi, val actions: Map<String, Action>)
    override fun onServiceConnected() { instance = this; notifyHost(true) }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; cancelBrowse?.invoke("UI_PERMISSION_REQUIRED"); main.removeCallbacksAndMessages(null); last = null; notifyHost(true); return super.onUnbind(intent) }
    override fun onInterrupt() { last = null; offlineContext = false; cancelBrowse?.invoke("UI_ACTION_INTERRUPTED"); notifyHost(true) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!eventPending) { eventPending = true; main.postDelayed({ eventPending = false; notifyHost() }, 500) }
    }
    private fun notifyHost(force: Boolean = false) {
        val current = readiness() ?: "ready"
        if (force || current != lastReadiness) { lastReadiness = current; HostService.instance?.sources?.externalStateChanged() }
    }
    fun readiness(): String? {
        if (!getSharedPreferences("apple-ui", MODE_PRIVATE).getBoolean("enabled", false)) return "UI_MODE_DISABLED"
        if (!getSystemService(PowerManager::class.java).isInteractive || getSystemService(KeyguardManager::class.java).isDeviceLocked) return "UI_SCREEN_UNLOCK_REQUIRED"
        val pi = runCatching { packageManager.getPackageInfo(PACKAGE, 0) }.getOrNull() ?: return "MISSING_SOURCE"
        if (pi.versionName != "6.5.3" || pi.longVersionCode != 1599L) return "UI_VERSION_UNVERIFIED"
        return null
    }
    private fun tree(): Tree {
        readiness()?.let { error(it) }
        val root = rootInActiveWindow ?: error("UI_FOREGROUND_REQUIRED")
        require(root.packageName?.toString() == PACKAGE) { "UI_FOREGROUND_REQUIRED" }
        val nodes = mutableListOf<UiNode>()
        val pending = ArrayDeque<Pair<AccessibilityNodeInfo, String>>(); pending.add(root to "0")
        while (pending.isNotEmpty()) {
            check(nodes.size < 400) { "UI_TREE_LIMIT" }
            val (node, path) = pending.removeFirst()
            if (node.packageName?.toString() != PACKAGE || node.isPassword || node.isEditable || !node.isVisibleToUser) continue
            val actions = node.actionList.map { it.id }
            nodes.add(UiNode(path, path.substringBeforeLast('.', "").takeIf { it.isNotEmpty() }, node.viewIdResourceName ?: "",
                node.text?.toString()?.take(512) ?: "", node.contentDescription?.toString()?.take(256) ?: "", node.className?.toString() ?: "",
                node.isClickable, node.isSelected, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD in actions,
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD in actions, node.isEnabled))
            require(path.count { it == '.' } <= 32 && node.childCount <= 100) { "UI_TREE_LIMIT" }
            for (i in 0 until node.childCount) node.getChild(i)?.let { pending.add(it to "$path.$i") }
        }
        return Tree(root.windowId, nodes)
    }
    private fun signature(node: UiNode) = "${node.id}\u0000${node.text}\u0000${node.description}\u0000${node.className}"
    fun debugSnapshot(): JSONObject {
        val current = tree()
        return JSONObject().put("window", current.window).put("validation", lastValidation ?: JSONObject.NULL).put("nodes", JSONArray().apply { current.nodes.forEach { n ->
            put(JSONObject().put("path", n.path).put("id", n.id).put("text", n.text).put("description", n.description)
                .put("class", n.className).put("clickable", n.clickable).put("selected", n.selected).put("forward", n.forward))
        } })
    }
    private fun perform(tree: Tree, path: String, kind: String, expectedSignature: String? = null) {
        val node = tree.nodes.firstOrNull { it.path == path } ?: error("STALE_LIBRARY")
        require(expectedSignature == null || expectedSignature == signature(node)) { "STALE_LIBRARY" }
        readiness()?.let { error(it) }
        var live = rootInActiveWindow ?: error("UI_FOREGROUND_REQUIRED")
        require(live.packageName?.toString() == PACKAGE && live.windowId == tree.window) { "STALE_LIBRARY" }
        for (index in path.split('.').drop(1)) live = live.getChild(index.toInt()) ?: error("STALE_LIBRARY")
        require(live.viewIdResourceName.orEmpty() == node.id && live.text?.toString().orEmpty().take(512) == node.text && live.contentDescription?.toString().orEmpty().take(256) == node.description) { "STALE_LIBRARY" }
        val action = if (kind == "scroll") AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_CLICK
        check(live.performAction(action)) { "UI_ACTION_FAILED" }
    }
    private fun frame(tree: Tree): Frame {
        val parsed = AppleUiModel.parse(tree.nodes, offlineContext)
        if (parsed.downloadedRoot) offlineContext = true
        val revision = "$session:${tree.window}:${parsed.fingerprint}"
        val actions = linkedMapOf<String, Action>()
        parsed.rows.take(50).forEachIndexed { i, row ->
            val raw = tree.nodes.first { it.path == row.path }
            actions["ui:$session:${parsed.fingerprint}:$i"] = Action(row.path, if (row.playable) "play" else if (row.browsable) "browse" else "disabled", signature(raw))
        }
        parsed.back?.let { path -> actions["ui:$session:${parsed.fingerprint}:back"] = Action(path, "back", signature(tree.nodes.first { it.path == path })) }
        if (!parsed.downloadedRoot) parsed.scroll?.let { path -> actions["ui:$session:${parsed.fingerprint}:scroll"] = Action(path, "scroll", signature(tree.nodes.first { it.path == path })) }
        return Frame(revision, tree.window, parsed, actions).also { last = it }
    }
    private fun result(frame: Frame, page: Int, end: Boolean = false): JSONObject {
        val entries = frame.actions.entries.filter { it.value.kind in setOf("play", "browse", "disabled") }
        val scroll = frame.actions.entries.firstOrNull { it.value.kind == "scroll" }?.key
        val back = frame.actions.entries.firstOrNull { it.value.kind == "back" }?.key
        return JSONObject().put("uiAssisted", true).put("title", frame.parsed.title).put("revision", frame.revision).put("page", page)
            .put("parent", scroll ?: "ui:current").put("backId", back ?: JSONObject.NULL).put("nextPage", if (!end && scroll != null) page + 1 else JSONObject.NULL)
            .put("items", JSONArray().apply { if (!end) entries.forEachIndexed { i, entry ->
                val row = frame.parsed.rows[i]
                put(JSONObject().put("id", entry.key).put("title", row.title).put("subtitle", row.subtitle).put("playable", row.playable).put("browsable", row.browsable))
            } })
    }
    private fun validate(item: String, revision: String, kind: String? = null): Pair<Tree, Action> {
        val previous = last ?: error("STALE_LIBRARY")
        require(revision == previous.revision) { "STALE_LIBRARY" }
        val action = previous.actions[item] ?: error("EXPIRED_ITEM")
        require(kind == null || action.kind == kind) { "UNSUPPORTED_CAPABILITY" }
        val current = tree(); val parsed = AppleUiModel.parse(current.nodes, offlineContext)
        if (current.window != previous.window || parsed.fingerprint != previous.parsed.fingerprint) {
            fun details(window: Int, value: ParsedAppleUi) = JSONObject().put("window", window).put("title", value.title)
                .put("fingerprint", value.fingerprint).put("back", value.back).put("scroll", value.scroll)
                .put("rows", JSONArray().apply { value.rows.forEach { row -> put(JSONObject().put("path", row.path)
                    .put("title", row.title).put("subtitle", row.subtitle).put("playable", row.playable).put("browsable", row.browsable)) } })
            // Bounded public-UI metadata for the debug receiver; never used to relax validation.
            lastValidation = JSONObject().put("kind", kind ?: action.kind).put("actionPath", action.path)
                .put("previous", details(previous.window, previous.parsed)).put("current", details(current.window, parsed))
            error("STALE_LIBRARY")
        }
        return current to action
    }
    fun validatePlayable(item: String, revision: String): String? = try { validate(item, revision, "play"); null } catch (e: Exception) { e.message ?: "UI_UNEXPECTED_SCREEN" }
    fun play(item: String, revision: String, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        if (busy) { done("UI_BUSY"); return }
        try {
            val (tree, action) = validate(item, revision, "play")
            require(stillAuthorized()) { "AUTHORIZATION_DENIED" }
            perform(tree, action.path, "play", action.signature); done(null)
        } catch (e: Exception) { done(e.message ?: "UI_ACTION_FAILED") }
    }
    fun browse(parent: String?, page: Int, revision: String?, stillAuthorized: () -> Boolean, done: (JSONObject?, String?) -> Unit) {
        if (busy) { done(null, "UI_BUSY"); return }
        busy = true
        var finished = false
        fun finish(value: JSONObject?, error: String?) { if (!finished) { finished = true; busy = false; cancelBrowse = null; if (error != null) last = null; done(value, error) } }
        cancelBrowse = { finish(null, it) }
        val deadline = android.os.SystemClock.elapsedRealtime() + 6000
        fun publish(end: Boolean = false) { try { require(stillAuthorized()) { "AUTHORIZATION_DENIED" }; finish(result(frame(tree()), page, end), null) } catch (e: Exception) { finish(null, e.message ?: "UI_UNEXPECTED_SCREEN") } }
        var settlingFingerprint: String? = null
        var settlingSince = 0L
        fun waitChanged(before: String) {
            if (finished) return
            if (!stillAuthorized()) { finish(null, "AUTHORIZATION_DENIED"); return }
            try {
                val current = tree(); val parsed = AppleUiModel.parse(current.nodes, offlineContext)
                if (parsed.fingerprint != before) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (settlingFingerprint != parsed.fingerprint) {
                        settlingFingerprint = parsed.fingerprint; settlingSince = now
                    } else if (now - settlingSince >= 450) {
                        finish(result(frame(current), page), null); return
                    }
                } else { settlingFingerprint = null }
                if (android.os.SystemClock.elapsedRealtime() >= deadline) { if (parent?.endsWith(":scroll") == true) publish(true) else finish(null, "UI_ACTION_TIMEOUT"); return }
                main.postDelayed({ waitChanged(before) }, 150)
            } catch (e: Exception) {
                // A transitioning window can be read again. Lost readiness is terminal and
                // must retain its actionable error rather than waiting out the UI deadline.
                val error = e.message ?: "UI_UNEXPECTED_SCREEN"
                if (error in setOf("UI_FOREGROUND_REQUIRED", "UI_UNEXPECTED_SCREEN", "UI_DOWNLOADED_CONTEXT_REQUIRED", "STALE_LIBRARY") && android.os.SystemClock.elapsedRealtime() < deadline) {
                    settlingFingerprint = null
                    main.postDelayed({ waitChanged(before) }, 150)
                } else finish(null, error)
            }
        }
        var rootSteps = 0
        fun root() {
            if (finished) return
            if (!stillAuthorized()) { finish(null, "AUTHORIZATION_DENIED"); return }
            if (android.os.SystemClock.elapsedRealtime() >= deadline || rootSteps++ > 20) { finish(null, "UI_SETUP_REQUIRED"); return }
            try {
                val current = tree()
                val downloaded = AppleUiModel.downloaded(current.nodes)
                if (downloaded != null) { val action = AppleUiModel.clickable(current.nodes, downloaded) ?: error("UI_ACTION_FAILED"); perform(current, action.path, "browse"); offlineContext = true; main.postDelayed({ root() }, 250); return }
                val parsed = runCatching { AppleUiModel.parse(current.nodes, offlineContext) }.getOrNull()
                if (parsed?.downloadedRoot == true) { publish(); return }
                val back = AppleUiModel.back(current.nodes)
                if (back != null && AppleUiModel.libraryTab(current.nodes)?.selected == true) { perform(current, back.path, "back"); main.postDelayed({ root() }, 250); return }
                val tab = AppleUiModel.libraryTab(current.nodes)
                if (tab == null) { main.postDelayed({ root() }, 250); return }
                val action = AppleUiModel.clickable(current.nodes, tab) ?: error("UI_ACTION_FAILED")
                perform(current, action.path, "browse"); main.postDelayed({ root() }, 250)
            } catch (e: Exception) {
                if (e.message == "UI_FOREGROUND_REQUIRED" || e.message == "STALE_LIBRARY") {
                    if (e.message == "UI_FOREGROUND_REQUIRED" && rootSteps == 1) {
                        val intent = packageManager.getLaunchIntentForPackage(PACKAGE) ?: return finish(null, "MISSING_SOURCE")
                        startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    main.postDelayed({ root() }, 250)
                } else finish(null, e.message ?: "UI_UNEXPECTED_SCREEN")
            }
        }
        try {
            readiness()?.let { error(it) }
            if (parent == null) { offlineContext = false; last = null; root() }
            else {
                val (current, action) = validate(parent, revision ?: "")
                require(action.kind in setOf("browse", "back", "scroll")) { "UNSUPPORTED_CAPABILITY" }
                val before = last!!.parsed.fingerprint
                require(stillAuthorized()) { "AUTHORIZATION_DENIED" }
                perform(current, action.path, action.kind, action.signature)
                main.postDelayed({ waitChanged(before) }, 200)
            }
        } catch (e: Exception) { finish(null, e.message ?: "UI_UNEXPECTED_SCREEN") }
    }
}

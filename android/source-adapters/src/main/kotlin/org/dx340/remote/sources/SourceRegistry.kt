package org.dx340.remote.sources

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import org.dx340.remote.library.RevisionGuard
import org.dx340.remote.library.LibraryIndex

/** Uses only public Android 13 APIs and our real application identity. */
class SourceRegistry(private val context: Context, private val listener: ComponentName, private val uiLibrary: UiLibraryAdapter? = null, private val mangoUiLibrary: UiLibraryAdapter? = null, private val changed: () -> Unit) {
    companion object {
        val PACKAGES = listOf("com.ibasso.music", "com.apple.android.music")
        const val MANGO_UI_ROOT = "mango-ui:root"
        const val MANGO_PLAYLIST_UI_ROOT = "mango-ui:playlists"
        const val MANGO_NATIVE_ROOT = "mango-native:root"
    }
    private val main = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val browsers = mutableMapOf<String, MediaBrowser>()
    private val browserStatus = mutableMapOf<String, String>()
    private val rootSamples = mutableMapOf<String, JSONObject>()
    private val controllers = mutableMapOf<String, MediaController>()
    private val epochs = mutableMapOf<String, String>()
    private val tokens = mutableMapOf<String, android.media.session.MediaSession.Token>()
    private val callbacks = mutableMapOf<String, MediaController.Callback>()
    private val callbackGuard = SourceCallbackGuard()
    private val artwork = ArtworkRepository()
    private val items = mutableMapOf<String, LinkedHashMap<String, MediaBrowser.MediaItem>>()
    private data class ItemProof(val parent: String, val page: Int, val fingerprint: String)
    private val proofs = mutableMapOf<String, LinkedHashMap<String, ItemProof>>()
    private val browseRequests = mutableSetOf<SourceRequest<JSONObject>>()
    private val revisionGuard = RevisionGuard()
    private val nativePages = NativeBrowsePageCache({ android.os.SystemClock.elapsedRealtime() })
    var selected = PACKAGES.first(); private set
    val accessError get() = sessionAccess.error
    private var closed = false
    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { refresh() }
    private val sessionAccess = SourceSessionAccess(
        { manager.addOnActiveSessionsChangedListener(sessionsChanged, listener, main) },
        { manager.removeOnActiveSessionsChangedListener(sessionsChanged) }, ::loseSessionAccess)
    val outputVolume = AndroidOutputVolume(context, main, { selected + "|" + epoch(selected) + "|" + version(selected) }, {
        !closed && accessError == null && controllers[selected]?.playbackState?.state == PlaybackState.STATE_PLAYING &&
            runCatching { manager.getActiveSessions(listener).none { it.packageName != selected && it.playbackState?.state !in listOf(PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED, PlaybackState.STATE_NONE) } }.getOrDefault(false)
    }, changed)
    private var storageInvalidations = 0L
    private val storage = StorageVolumeObserver(context, ::invalidateStorageLibrary)

    fun start() {
        storage.start(); refresh(); probeBrowsers()
    }
    fun close() {
        closed = true
        storage.close()
        PACKAGES.forEach(nativePages::invalidate)
        outputVolume.close()
        callbackGuard.close()
        browseRequests.toList().forEach { it.cancel("SOURCE_NOT_READY") }
        main.removeCallbacksAndMessages(null)
        sessionAccess.close()
        controllers.forEach { (p, c) -> callbacks[p]?.let { c.unregisterCallback(it) } }
        browsers.values.forEach { it.disconnect() }; browsers.clear()
    }
    /** Main-thread boundary; deliberately leaves media sessions and playback intact. */
    fun invalidateStorageLibrary() {
        if (closed) return
        if (storageInvalidations < Long.MAX_VALUE) storageInvalidations++
        // Retire every source context before cancellation can run reentrant callbacks.
        PACKAGES.forEach { epochs[it] = UUID.randomUUID().toString() }
        items.clear(); proofs.clear(); rootSamples.clear(); revisionGuard.clear()
        PACKAGES.forEach(nativePages::invalidate)
        uiLibrary?.invalidateLibrary(); mangoUiLibrary?.invalidateLibrary()
        browseRequests.toList().forEach { it.cancel("STALE_LIBRARY") }
        outputVolume.control.reset()
    }
    fun storageDiagnostic(): JSONObject = storage.diagnostic().put("libraryInvalidations", storageInvalidations)
    private fun attach(pkg: String, controller: MediaController) {
        if (closed || accessError != null) return
        if (tokens[pkg] == controller.sessionToken) return
        val lease = callbackGuard.replace(pkg)
        callbacks[pkg]?.let { controllers[pkg]?.unregisterCallback(it) }
        epochs[pkg] = UUID.randomUUID().toString(); tokens[pkg] = controller.sessionToken
        items.remove(pkg); proofs.remove(pkg); nativePages.invalidate(pkg)
        controllers[pkg] = controller
        val cb = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) = callbackGuard.dispatch(lease, changed)
            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) = callbackGuard.dispatch(lease, changed)
            override fun onQueueChanged(queue: MutableList<android.media.session.MediaSession.QueueItem>?) = callbackGuard.dispatch(lease, changed)
            override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) = callbackGuard.dispatch(lease, changed)
            override fun onSessionDestroyed() {
                if (!callbackGuard.retire(lease)) return
                controllers.remove(pkg); tokens.remove(pkg); callbacks.remove(pkg)
                epochs[pkg] = UUID.randomUUID().toString(); items.remove(pkg); proofs.remove(pkg); nativePages.invalidate(pkg); changed()
            }
        }
        callbacks[pkg] = cb; controller.registerCallback(cb, main)
    }
    fun refresh() {
        if (closed) return
        try {
            // Startup can precede the user's grant. Retry registration on recovery.
            sessionAccess.refresh { manager.getActiveSessions(listener).filter { it.packageName in PACKAGES } }?.let { live ->
                live.forEach { attach(it.packageName, it) }
                browsers.forEach { (pkg, browser) ->
                    if (pkg !in controllers && browser.isConnected) attach(pkg, MediaController(context, browser.sessionToken))
                }
            }
        } catch (_: SecurityException) { sessionAccess.permissionDenied() }
        changed()
    }
    private fun loseSessionAccess() {
        // Stop pending native callbacks even if permission is restored before they arrive.
        browseRequests.toList().forEach { it.cancel("PERMISSION_REVOKED") }
        callbackGuard.invalidateAll()
        controllers.forEach { (pkg, controller) -> callbacks[pkg]?.let { runCatching { controller.unregisterCallback(it) } } }
        controllers.clear(); tokens.clear(); callbacks.clear(); items.clear(); proofs.clear(); rootSamples.clear()
        PACKAGES.forEach(nativePages::invalidate)
        PACKAGES.forEach { epochs[it] = UUID.randomUUID().toString() }
        outputVolume.control.reset()
    }
    fun probeBrowsers() {
        if (closed) return
        for (pkg in PACKAGES) {
            if (browsers.containsKey(pkg)) continue
            val services = context.packageManager.queryIntentServices(Intent("android.media.browse.MediaBrowserService").setPackage(pkg), PackageManager.GET_META_DATA)
            val service = services.firstOrNull { it.serviceInfo.exported }
            if (service == null) { browserStatus[pkg] = "NO_EXPORTED_BROWSER"; continue }
            val component = ComponentName(pkg, service.serviceInfo.name)
            browserStatus[pkg] = "CONNECTING"
            val browser = MediaBrowser(context, component, object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() {
                    if (closed) return
                    val b = browsers.getValue(pkg)
                    attach(pkg, MediaController(context, b.sessionToken))
                    browserStatus[pkg] = "CONNECTED"; changed()
                    val probeEpoch = epoch(pkg)
                    val callback = object : MediaBrowser.SubscriptionCallback() {
                        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
                            if (closed) return
                            b.unsubscribe(parentId, this)
                            if (epoch(pkg) != probeEpoch) return
                            rootSamples[pkg] = JSONObject().put("root", parentId).put("count", children.size).put("items", JSONArray().apply {
                                children.take(50).forEach { put(JSONObject().put("id", it.mediaId).put("title", it.description.title?.toString()).put("browsable", it.isBrowsable).put("playable", it.isPlayable)) }
                            }); changed()
                        }
                        override fun onError(parentId: String) { if (closed) return; b.unsubscribe(parentId, this); if (epoch(pkg) != probeEpoch) return; rootSamples[pkg] = JSONObject().put("error", "BROWSE_DENIED"); changed() }
                    }
                    b.subscribe(b.root, callback)
                    main.postDelayed({
                        if (closed) return@postDelayed
                        runCatching { if (b.isConnected) b.unsubscribe(b.root, callback) }
                        if (epoch(pkg) == probeEpoch && !rootSamples.containsKey(pkg)) { rootSamples[pkg] = JSONObject().put("error", "SOURCE_TIMEOUT"); changed() }
                    }, 8000)
                }
                override fun onConnectionFailed() { if (closed) return; nativePages.invalidate(pkg); browserStatus[pkg] = "CLIENT_DENIED_OR_FAILED"; changed() }
                override fun onConnectionSuspended() { if (closed) return; nativePages.invalidate(pkg); browserStatus[pkg] = "SUSPENDED"; changed() }
            }, null)
            browsers[pkg] = browser
            try { browser.connect() } catch (e: Exception) { browserStatus[pkg] = "FAILED:${e.javaClass.simpleName}" }
        }
        changed()
    }
    fun epoch(pkg: String) = epochs.getOrPut(pkg) { UUID.randomUUID().toString() }
    fun externalStateChanged() = changed()
    private fun usingUi(pkg: String) = pkg == PACKAGES[1] && uiLibrary?.enabled == true && browsers[pkg]?.isConnected != true
    private fun supports(pkg: String, flag: Long) = ((controllers[pkg]?.playbackState?.actions ?: 0L) and flag) != 0L
    fun capabilities(pkg: String): JSONObject = JSONObject().apply {
        val sessionReady = !closed && accessError == null
        val verification = context.getSharedPreferences("source-verification", Context.MODE_PRIVATE)
        put("canPlay", supports(pkg, PlaybackState.ACTION_PLAY) || (pkg == PACKAGES[0] && verification.getString("mangoObservedPlayVersion", null) == version(pkg)))
        put("canPause", supports(pkg, PlaybackState.ACTION_PAUSE) || (pkg == PACKAGES[0] && verification.getString("mangoPauseVersion", null) == version(pkg)))
        put("canNext", supports(pkg, PlaybackState.ACTION_SKIP_TO_NEXT)); put("canPrevious", supports(pkg, PlaybackState.ACTION_SKIP_TO_PREVIOUS))
        put("canSeek", supports(pkg, PlaybackState.ACTION_SEEK_TO)); put("canReadQueue", controllers[pkg]?.queue != null)
        put("canPlayQueueItem", supports(pkg, PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM))
        put("canBrowse", browsers[pkg]?.isConnected == true || (usingUi(pkg) && uiLibrary?.ready == true))
        put("canPlayNativeItem", nativeSelectionSupported(pkg))
        put("canPlayItem", nativeSelectionSupported(pkg) || (usingUi(pkg) && uiLibrary?.ready == true) || (pkg == PACKAGES[0] && mangoUiLibrary?.ready == true))
        put("browseMode", if (usingUi(pkg)) "ui-assisted" else "direct")
        put("canBrowseFolders", pkg == PACKAGES[0] && mangoUiLibrary?.ready == true)
        if (pkg == PACKAGES[0] && mangoUiLibrary?.enabled == true) put("uiBrowseReason", mangoUiLibrary.explanation)
        put("canSearch", false); put("canAdjustVolume", pkg == selected && outputVolume.state().optBoolean("verified")); put("canResume", false); put("canColdLaunch", false)
        put("canShuffle", false); put("canRepeat", false)
        put("reason", if (usingUi(pkg)) uiLibrary?.explanation else "Actions reflect advertised/observed source APIs; physical acceptance remains separate.")
        if (!storage.active) {
            put("canPlayNativeItem", false); put("canPlayItem", false); put("canPlayQueueItem", false)
            put("reason", "Storage-change monitoring unavailable. Restart the companion before selecting library or queue items.")
        }
        if (!sessionReady) {
            // A connected browser or saved Mango verification must not keep controls enabled.
            keys().asSequence().filter { it.startsWith("can") }.toList().forEach { put(it, false) }
            remove("uiBrowseReason")
            put("reason", "On DX340, enable playback session access in Notification access settings, then refresh.")
        }
    }
    fun sources(): JSONArray = JSONArray().apply { PACKAGES.forEach { pkg ->
        val version = runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()
        put(JSONObject().put("id", pkg).put("name", if (pkg == PACKAGES[0]) "Mango Player" else "Apple Music")
            .put("installed", version != null).put("version", version ?: "not installed").put("sourceEpoch", epoch(pkg))
            .put("browser", browserStatus[pkg] ?: "NOT_PROBED").put("rootProbe", rootSamples[pkg] ?: JSONObject.NULL).put("capabilities", capabilities(pkg)))
    } }
    fun state(): JSONObject {
        val c = controllers[selected]; val m = c?.metadata; val p = c?.playbackState
        val duration = (m?.getLong("android.media.metadata.DURATION") ?: 0).coerceAtLeast(0)
        val progress = PlaybackPosition.snapshot(p?.position ?: -1, duration,
            p?.playbackSpeed?.toDouble() ?: 0.0, p?.state == PlaybackState.STATE_PLAYING,
            p?.lastPositionUpdateTime ?: 0, android.os.SystemClock.elapsedRealtime())
        val artKey = artwork.remember(selected, m?.getBitmap("android.media.metadata.ART") ?: m?.getBitmap("android.media.metadata.ALBUM_ART") ?: m?.description?.iconBitmap)
        return JSONObject().put("source", selected).put("sourceEpoch", epoch(selected)).put("artworkKey", artKey ?: JSONObject.NULL)
            .put("title", m?.getString("android.media.metadata.TITLE") ?: "No active track")
            .put("artist", m?.getString("android.media.metadata.ARTIST") ?: "")
            .put("album", m?.getString("android.media.metadata.ALBUM") ?: "")
            .put("duration", duration).put("position", progress.position).put("playbackSpeed", progress.speed)
            .put("playing", p?.state == PlaybackState.STATE_PLAYING)
            .put("playbackState", p?.state ?: 0).put("capabilities", capabilities(selected))
            .put("volume", outputVolume.state())
            .put("accessError", accessError ?: JSONObject.NULL)
    }
    fun select(pkg: String) {
        require(pkg in PACKAGES) { "MISSING_SOURCE" }
        if (selected != pkg) PACKAGES.forEach(nativePages::invalidate)
        selected = pkg; changed()
    }
    fun observedSources(): JSONArray = JSONArray().apply { controllers.forEach { (pkg, c) ->
        put(JSONObject().put("source", pkg).put("sourceEpoch", epoch(pkg))
            .put("title", c.metadata?.getString("android.media.metadata.TITLE") ?: "")
            .put("playbackState", c.playbackState?.state ?: PlaybackState.STATE_NONE)
            .put("playing", c.playbackState?.state == PlaybackState.STATE_PLAYING))
    } }
    /** Public browser metadata only, for local developer diagnostics; never reads a file. */
    fun nativeItemEvidence(id: String): JSONObject {
        require(selected == PACKAGES[0]) { "WRONG_SOURCE" }
        val item = items[selected]?.get(id) ?: error("EXPIRED_ITEM")
        val description = item.description
        return JSONObject().put("id", item.mediaId).put("title", description.title?.toString())
            .put("subtitle", description.subtitle?.toString())
            .put("mediaUri", description.mediaUri?.toString() ?: JSONObject.NULL)
            .put("extraKeys", JSONArray(description.extras?.keySet()?.sorted()?.take(50) ?: emptyList<String>()))
            .put("note", "Source-provided public description; no filesystem or private app storage read")
    }
    fun getArtwork(key: String): JSONObject {
        require(key == state().optString("artworkKey")) { "EXPIRED_ITEM" }
        val bytes = artwork.get(key) ?: error("EXPIRED_ITEM")
        return JSONObject().put("key", key).put("mime", "image/jpeg").put("base64", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
    }
    private fun fingerprint(item: MediaBrowser.MediaItem): String {
        val description = item.description
        return NativeItemFingerprint.of(item.mediaId, item.flags, description.title?.toString(),
            description.subtitle?.toString(), description.description?.toString(), description.mediaUri?.toString())
    }
    fun playItem(args: JSONObject, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        if (!storage.active) { done("SOURCE_NOT_READY"); return }
        val pkg = selected
        val deadline = android.os.SystemClock.elapsedRealtime() + 10000
        if (usingUi(pkg) || (pkg == PACKAGES[0] && args.optString("item").startsWith("mango-ui:"))) {
            val ui = (if (pkg == PACKAGES[0]) mangoUiLibrary else uiLibrary) ?: return done("UI_PERMISSION_REQUIRED")
            val id = args.optString("item"); val revision = args.optString("revision")
            val invalid = ui.validatePlayable(id, revision)
            if (invalid != null) { done(invalid); return }
            if (controllers[pkg] == null) { done("SOURCE_NOT_READY"); return }
            val expected = epoch(pkg)
            preparePlayback(pkg, expected, stillAuthorized) { error ->
                if (error != null) done(error)
                else if (selected != pkg || epoch(pkg) != expected) done("STALE_SOURCE")
                else ui.play(id, revision, { playbackGuard(pkg, expected, stillAuthorized) == null }, done)
            }
            return
        }
        try {
            require(nativeSelectionSupported(pkg)) { "UNSUPPORTED_CAPABILITY" }
            require(args.getString("revision") == epoch(pkg)) { "STALE_LIBRARY" }
            val id = args.getString("item")
            val proof = proofs[pkg]?.get(id) ?: error("EXPIRED_ITEM")
            val expectedEpoch = epoch(pkg)
            browseAuthorized(proof.parent, proof.page, null, stillAuthorized) { data, browseError ->
                try {
                    require(browseError == null) { browseError ?: "SOURCE_TIMEOUT" }
                    require(selected == pkg && epoch(pkg) == expectedEpoch) { "STALE_SOURCE" }
                    val list = data?.getJSONArray("items") ?: error("EXPIRED_ITEM")
                    require((0 until list.length()).any { list.getJSONObject(it).getString("id") == id }) { "EXPIRED_ITEM" }
                    revisionGuard.validateSelection(proof.fingerprint, items[pkg]?.get(id)?.let(::fingerprint))
                    require(stillAuthorized()) { "AUTHORIZATION_DENIED" }
                    require(nativeSelectionSupported(pkg)) { "UNSUPPORTED_CAPABILITY" }
                    preparePlayback(pkg, expectedEpoch, stillAuthorized, deadline) { handoffError ->
                        if (handoffError != null) done(handoffError)
                        else browseAuthorized(proof.parent, proof.page, null, stillAuthorized) { _, finalError ->
                            try {
                                require(finalError == null) { finalError ?: "SOURCE_TIMEOUT" }
                                require(android.os.SystemClock.elapsedRealtime() <= deadline) { "SOURCE_TIMEOUT" }
                                playbackGuard(pkg, expectedEpoch, stillAuthorized)?.let { error(it) }
                                revisionGuard.validateSelection(proof.fingerprint, items[pkg]?.get(id)?.let(::fingerprint))
                                command("PLAY_ITEM_VALIDATED", args); done(null)
                            } catch (e: Exception) { done(e.message ?: "SOURCE_NOT_READY") }
                        }
                    }
                } catch (e: Exception) { done(e.message ?: "SOURCE_NOT_READY") }
            }
        } catch (e: Exception) { done(e.message ?: "SOURCE_NOT_READY") }
    }
    private fun playbackGuard(pkg: String, expected: String, stillAuthorized: () -> Boolean): String? = when {
        closed || controllers[pkg] == null -> "SOURCE_NOT_READY"
        accessError != null -> "PERMISSION_REVOKED"
        !stillAuthorized() -> "AUTHORIZATION_DENIED"
        selected != pkg || epoch(pkg) != expected -> "STALE_SOURCE"
        else -> null
    }
    private fun preparePlayback(target: String, expected: String, stillAuthorized: () -> Boolean, deadline: Long = Long.MAX_VALUE, done: (String?) -> Unit) {
        PlaybackHandoff({ android.os.SystemClock.elapsedRealtime() }, { delay, task -> main.postDelayed(task, delay) }, {
            controllers.map { (pkg, c) ->
                val observation = when (c.playbackState?.state) {
                    PlaybackState.STATE_PAUSED -> HandoffPlayback.PAUSED
                    PlaybackState.STATE_NONE, PlaybackState.STATE_STOPPED -> HandoffPlayback.STOPPED
                    PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING,
                    PlaybackState.STATE_FAST_FORWARDING, PlaybackState.STATE_REWINDING,
                    PlaybackState.STATE_SKIPPING_TO_NEXT, PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
                    PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> HandoffPlayback.ACTIVE
                    else -> HandoffPlayback.UNKNOWN
                }
                HandoffSource(pkg, epoch(pkg), observation, capabilities(pkg).optBoolean("canPause"))
            }
        }, { previous ->
            require(epoch(previous.id) == previous.epoch) { "SOURCE_HANDOFF_FAILED" }
            val controller = controllers[previous.id] ?: error("SOURCE_HANDOFF_FAILED")
            controller.transportControls.pause()
        }).prepare(target, {
            if (android.os.SystemClock.elapsedRealtime() > deadline) "SOURCE_TIMEOUT"
            else playbackGuard(target, expected, stillAuthorized)
        }, done)
    }
    fun playQueueItem(args: JSONObject, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        val pkg = selected; val expected = epoch(pkg)
        fun validate() {
            playbackGuard(pkg, expected, stillAuthorized)?.let { error(it) }
            require(capabilities(pkg).optBoolean("canPlayQueueItem")) { "UNSUPPORTED_CAPABILITY" }
            val queue = controllers[pkg]?.queue ?: error("SOURCE_NOT_READY")
            require(args.getString("revision") == queueRevision(queue)) { "STALE_LIBRARY" }
            require(queue.any { it.queueId == args.getLong("item") }) { "EXPIRED_ITEM" }
        }
        try {
            validate()
            preparePlayback(pkg, expected, stillAuthorized) { handoffError ->
                if (handoffError != null) done(handoffError)
                else try { validate(); command("PLAY_QUEUE_ITEM", args); done(null) }
                catch (e: Exception) { done(e.message ?: "SOURCE_NOT_READY") }
            }
        } catch (e: Exception) { done(e.message ?: "SOURCE_NOT_READY") }
    }
    fun setPlaying(playing: Boolean, experimental: Boolean = false, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        val pkg = selected; val expectedEpoch = epoch(pkg)
        try {
            require(accessError == null) { "PERMISSION_REVOKED" }
            val controller = controllers[pkg] ?: error("SOURCE_NOT_READY")
            val desired = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val current = controller.playbackState?.state
            require(current == PlaybackState.STATE_PLAYING || current == PlaybackState.STATE_PAUSED) { "SOURCE_NOT_READY" }
            require(current == desired || experimental || capabilities(pkg).optBoolean(if (playing) "canPlay" else "canPause")) { "UNSUPPORTED_CAPABILITY" }
            fun dispatch() {
                try {
                    playbackGuard(pkg, expectedEpoch, stillAuthorized)?.let { error(it) }
                    val latest = controller.playbackState?.state
                    require(latest == PlaybackState.STATE_PLAYING || latest == PlaybackState.STATE_PAUSED) { "SOURCE_NOT_READY" }
                    if (latest == desired) { done(null); return }
                    require(experimental || capabilities(pkg).optBoolean(if (playing) "canPlay" else "canPause")) { "UNSUPPORTED_CAPABILITY" }
                    // Mango's onPlay can toggle. Dispatch once from observed opposite state only.
                    if (playing) controller.transportControls.play() else controller.transportControls.pause()
                    val deadline = android.os.SystemClock.elapsedRealtime() + 3500
                    fun observe() {
                        val invalid = playbackGuard(pkg, expectedEpoch, stillAuthorized)
                        when {
                            invalid != null -> done(invalid)
                            controller.playbackState?.state == desired -> done(null)
                            android.os.SystemClock.elapsedRealtime() >= deadline -> done("SOURCE_TIMEOUT")
                            else -> main.postDelayed({ observe() }, 100)
                        }
                    }
                    main.postDelayed({ observe() }, 100)
                } catch (e: Exception) { done(e.message ?: "SOURCE_NOT_READY") }
            }
            if (playing) {
                preparePlayback(pkg, expectedEpoch, stillAuthorized) { error ->
                    if (error != null) done(error) else dispatch()
                }
            } else dispatch()
        } catch (e: Exception) { done(e.message ?: "SOURCE_NOT_READY") }
    }
    private fun version(pkg: String) = runCatching { context.packageManager.getPackageInfo(pkg, 0).let { "${it.versionName}|${it.longVersionCode}|${android.os.Build.FINGERPRINT}" } }.getOrNull() ?: "missing"
    // Native item IDs must come from a connected browser and are revalidated there before play.
    // A session action bit alone must not keep assisted item selection enabled after UI loss.
    private fun nativeSelectionSupported(pkg: String) = storage.active && browsers[pkg]?.isConnected == true &&
        (supports(pkg, PlaybackState.ACTION_PLAY_FROM_MEDIA_ID) || verifiedPlayItem(pkg))
    private fun verifiedPlayItem(pkg: String): Boolean {
        if (pkg != PACKAGES[0]) return false
        val prefs = context.getSharedPreferences("source-verification", Context.MODE_PRIVATE)
        // Older confirmation used raw source IDs. It cannot prove the corrected
        // zero-based dispatch scheme; require a new explicit local listening check.
        if (prefs.getString("mangoNativeSelectionScheme", null) != MangoNativePlaybackId.SCHEME) return false
        val saved = prefs.getString("mangoPlayItemVersion", null) ?: return false
        val current = version(pkg)
        if (saved == current) return true
        // Migrate the first installed beta's user-confirmed record; never infer success from dispatch.
        val installedName = runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()
        if (saved != installedName) return false
        return runCatching {
            val file = java.io.File(context.filesDir, "mango-playback-tests.jsonl")
            if (!file.isFile || file.length() > 1024 * 1024) return@runCatching false
            val proof = JSONObject(file.useLines { lines -> lines.lastOrNull() } ?: return@runCatching false)
            if (proof.optString("result") != "USER_CONFIRMED_AUDIBLE" || proof.optString("source") != pkg || proof.optString("version") != installedName || proof.optString("build") != android.os.Build.DISPLAY || proof.optString("device") != android.os.Build.MODEL) return@runCatching false
            prefs.edit().putString("mangoPlayItemVersion", current).commit()
        }.getOrDefault(false)
    }
    fun confirmMangoPlayItem(success: Boolean) {
        require(selected == PACKAGES[0]) { "WRONG_SOURCE" }
        val previous = nativeSelectionSupported(selected)
        check(context.getSharedPreferences("source-verification", Context.MODE_PRIVATE).edit().apply {
            if (success) {
                putString("mangoPlayItemVersion", version(selected))
                putString("mangoNativeSelectionScheme", MangoNativePlaybackId.SCHEME)
            } else {
                remove("mangoPlayItemVersion"); remove("mangoNativeSelectionScheme")
            }
        }.commit()) { "VERIFICATION_STORE_FAILED" }
        // Native clients clear visible/cached rows when sourceEpoch changes. Keep the
        // same-session parent proofs so the local test can refresh its current folder.
        if (previous != nativeSelectionSupported(selected)) epochs[selected] = UUID.randomUUID().toString()
        changed()
    }
    /** Local diagnostic experiment only. Does not imply playback or enable capability by itself. */
    fun experimentMangoPlayItem(itemId: String, revision: String) {
        require(selected == PACKAGES[0]) { "WRONG_SOURCE" }
        require(revision == epoch(selected)) { "STALE_LIBRARY" }
        val item = items[selected]?.get(itemId) ?: error("EXPIRED_ITEM")
        require(item.isPlayable) { "UNSUPPORTED_CAPABILITY" }
        val controller = controllers[selected] ?: error("SOURCE_NOT_READY")
        val other = try { manager.getActiveSessions(listener).firstOrNull { it.packageName != selected && it.playbackState?.state == PlaybackState.STATE_PLAYING } } catch (_: SecurityException) { error("PERMISSION_REVOKED") }
        check(other == null) { "Pause ${other?.packageName} on DX340 before the test." }
        controller.transportControls.playFromMediaId(nativePlaybackId(selected, item), null)
    }
    private fun nativePlaybackId(pkg: String, item: MediaBrowser.MediaItem): String? {
        if (pkg != PACKAGES[0]) return item.mediaId
        val id = item.mediaId ?: error("EXPIRED_ITEM")
        val proof = proofs[pkg]?.get(id) ?: error("EXPIRED_ITEM")
        val installed = context.packageManager.getPackageInfo(pkg, 0)
        return MangoNativePlaybackId.resolve(installed.versionName, installed.longVersionCode,
            proof.parent, id, item.description.title?.toString() ?: "")
    }
    fun command(op: String, args: JSONObject) {
        require(accessError == null) { "PERMISSION_REVOKED" }
        val c = controllers[selected] ?: error("SOURCE_NOT_READY")
        val controls = c.transportControls
        val cap = capabilities(selected)
        val key = when (op) {
            "NEXT" -> "canNext"; "PREVIOUS" -> "canPrevious"; "SEEK" -> "canSeek"
            "PLAY_ITEM_VALIDATED" -> "canPlayItem"; "PLAY_QUEUE_ITEM" -> "canPlayQueueItem"
            else -> error("UNSUPPORTED_CAPABILITY")
        }
        require(cap.optBoolean(key)) { "UNSUPPORTED_CAPABILITY" }
        // Final defense after asynchronous preparation and item/queue revalidation.
        if (op == "PLAY_ITEM_VALIDATED" || op == "PLAY_QUEUE_ITEM") {
            require(controllers.none { (pkg, other) -> pkg != selected && other.playbackState?.state == PlaybackState.STATE_PLAYING }) { "OTHER_SOURCE_PLAYING" }
        }
        when (op) {
            "NEXT" -> controls.skipToNext(); "PREVIOUS" -> controls.skipToPrevious()
            "SEEK" -> { val ms = args.getLong("position"); require(ms >= 0 && ms <= (c.metadata?.getLong("android.media.metadata.DURATION") ?: 0)) { "INVALID_ARGUMENT" }; controls.seekTo(ms) }
            "PLAY_ITEM_VALIDATED" -> {
                require(nativeSelectionSupported(selected)) { "UNSUPPORTED_CAPABILITY" }
                require(args.getString("revision") == epoch(selected)) { "STALE_LIBRARY" }
                val item = items[selected]?.get(args.getString("item")) ?: error("EXPIRED_ITEM")
                require(item.isPlayable) { "UNSUPPORTED_CAPABILITY" }; controls.playFromMediaId(nativePlaybackId(selected, item), null)
            }
            "PLAY_QUEUE_ITEM" -> {
                require(args.getString("revision") == queueRevision(c.queue ?: emptyList())) { "STALE_LIBRARY" }
                val id = args.getLong("item"); require(c.queue?.any { it.queueId == id } == true) { "EXPIRED_ITEM" }
                controls.skipToQueueItem(id)
            }
        }
    }
    fun queue(): JSONArray = JSONArray().apply { controllers[selected]?.queue?.take(50)?.forEach { q ->
        put(JSONObject().put("id", q.queueId.toString()).put("title", q.description.title?.toString() ?: "Untitled"))
    } }
    private fun queueRevision(queue: List<android.media.session.MediaSession.QueueItem>): String {
        val content = epoch(selected) + "\u0000" + queue.joinToString("\u0000") { "${it.queueId}:${it.description.mediaId}:${it.description.mediaUri}:${it.description.title}" }
        return java.security.MessageDigest.getInstance("SHA-256").digest(content.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    fun queueState(args: JSONObject = JSONObject()): JSONObject {
        val queue = controllers[selected]?.queue ?: error("UNSUPPORTED_CAPABILITY")
        require(queue.size <= QueuePages.MAX_ITEMS) { "QUEUE_LIMIT" }
        return QueuePages.render(queue.map { QueuePages.Entry(it.queueId.toString(), it.description.title?.toString() ?: "Untitled") }, queueRevision(queue), args)
    }
    fun browse(parent: String?, page: Int, expectedRevision: String? = null, done: (JSONObject?, String?) -> Unit) {
        browseAuthorized(parent, page, expectedRevision, { true }, done)
    }
    fun browseAuthorized(parent: String?, page: Int, expectedRevision: String?, stillAuthorized: () -> Boolean, done: (JSONObject?, String?) -> Unit) {
        require(page in 0..10000) { "INVALID_ARGUMENT" }
        val pkg = selected; val revision = epoch(pkg)
        fun invalid(): String? = when {
            !stillAuthorized() -> "AUTHORIZATION_DENIED"
            closed -> "SOURCE_NOT_READY"
            accessError != null -> "PERMISSION_REVOKED"
            selected != pkg || epoch(pkg) != revision -> "STALE_LIBRARY"
            else -> null
        }
        val allowed = { invalid() == null }
        invalid()?.let { done(null, it); return }
        if (usingUi(pkg)) { uiLibrary!!.browse(parent, page, expectedRevision, allowed, done); return }
        if (pkg == PACKAGES[0] && parent?.startsWith("mango-ui:") == true) {
            val ui = mangoUiLibrary ?: return done(null, "UI_PERMISSION_REQUIRED")
            ui.browse(if (parent == MANGO_UI_ROOT) null else parent, page, expectedRevision, allowed, done); return
        }
        val nativeRoot = pkg == PACKAGES[0] && parent == MANGO_NATIVE_ROOT
        require(nativeRoot || expectedRevision == null || expectedRevision == revision) { "STALE_LIBRARY" }
        val browser = browsers[pkg]?.takeIf { it.isConnected } ?: return done(null, "BROWSE_UNAVAILABLE")
        val target = if (nativeRoot) browser.root else parent ?: browser.root
        if (target != browser.root && items[pkg]?.get(target)?.isBrowsable != true) return done(null, "EXPIRED_ITEM")
        // A root refresh always queries the source and drops old presentation pages.
        // Playback revalidation deliberately passes no revision, bypassing this cache twice.
        if (target == browser.root && (nativeRoot || expectedRevision == null)) nativePages.invalidate(pkg)
        val cacheKey = LibraryIndex.Key(pkg, "$revision|${version(pkg)}", target, page)
        fun render(rows: List<NativeBrowsePageCache.Row>, nextPage: Int?, cached: Boolean, ageMs: Long = 0): JSONObject {
            val result = JSONArray()
            val nativeSelection = nativeSelectionSupported(pkg)
            if (pkg == PACKAGES[0] && target == browser.root && page == 0 && mangoUiLibrary?.enabled == true) {
                result.put(JSONObject().put("id", MANGO_UI_ROOT).put("title", "Local folders · assisted")
                    .put("subtitle", mangoUiLibrary.explanation).put("browsable", mangoUiLibrary.ready).put("playable", false).put("uiAssisted", true))
                result.put(JSONObject().put("id", MANGO_PLAYLIST_UI_ROOT).put("title", "Playlists · assisted")
                    .put("subtitle", mangoUiLibrary.explanation).put("browsable", mangoUiLibrary.ready).put("playable", false).put("uiAssisted", true))
            }
            rows.forEach { row ->
                result.put(JSONObject().put("id", row.id).put("title", row.title).put("subtitle", row.subtitle)
                    .put("browsable", row.browsable).put("playable", row.playable && nativeSelection)
                    .put("nativeTestable", pkg == PACKAGES[0] && row.playable))
            }
            return JSONObject().put("parent", target).put("revision", revision).put("page", page)
                .put("nextPage", nextPage ?: JSONObject.NULL).put("items", result)
                .put("cached", cached).put("cacheAgeMs", ageMs)
        }
        val cached = nativePages.read(cacheKey, expectedRevision == revision && target != browser.root) { row ->
            val proof = proofs[pkg]?.get(row.id)
            proof?.parent == target && proof.page == page && proof.fingerprint == row.fingerprint &&
                items[pkg]?.get(row.id)?.let(::fingerprint) == row.fingerprint
        }
        if (cached != null) {
            invalid()?.let { done(null, it); return }
            done(render(cached.rows, cached.nextPage, true, android.os.SystemClock.elapsedRealtime() - cached.observedAt), null)
            return
        }
        val options = Bundle().apply { putInt(MediaBrowser.EXTRA_PAGE, page); putInt(MediaBrowser.EXTRA_PAGE_SIZE, 25) }
        lateinit var cb: MediaBrowser.SubscriptionCallback
        lateinit var request: SourceRequest<JSONObject>
        request = SourceRequest({ invalid() ?: if (browsers[pkg] !== browser || !browser.isConnected) "BROWSE_UNAVAILABLE" else null }, {
            browseRequests.remove(request)
            // A disconnect may race unsubscribe; completion must still reach the caller.
            runCatching { if (browser.isConnected) browser.unsubscribe(target, cb) }
        }, done)
        fun finish(value: JSONObject?, error: String?) {
            request.finish(value, error)
        }
        cb = object : MediaBrowser.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>, opts: Bundle) = loaded(children)
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) = loaded(children)
            private fun loaded(children: List<MediaBrowser.MediaItem>) {
                if (!request.check()) return
                if (children.size > 10000) { finish(null, "SOURCE_LIBRARY_LIMIT"); return }
                // Legacy services may ignore paging options. Paginate their bounded result on DX340.
                val visible = if (children.size > 25) children.drop(page * 25).take(25) else children
                val rows = visible.mapNotNull { item ->
                    val id = item.mediaId ?: return@mapNotNull null
                    if (id.toByteArray(Charsets.UTF_8).size > 512) { finish(null, "SOURCE_ID_TOO_LARGE"); return }
                    NativeBrowsePageCache.Row(id, item.description.title?.toString()?.take(128) ?: "Untitled",
                        item.description.subtitle?.toString()?.take(64) ?: "", item.isBrowsable, item.isPlayable, fingerprint(item))
                }
                if (rows.map { it.id }.distinct().size != rows.size) { finish(null, "SOURCE_ID_AMBIGUOUS"); return }
                if (revisionGuard.update("$pkg\u0000$target\u0000$page", visible.associate { (it.mediaId ?: "") to fingerprint(it) })) {
                    epochs[pkg] = UUID.randomUUID().toString(); items.remove(pkg); proofs.remove(pkg); nativePages.invalidate(pkg); changed()
                    finish(null, "STALE_LIBRARY"); return
                }
                val known = items.getOrPut(pkg) { linkedMapOf() }
                val knownProofs = proofs.getOrPut(pkg) { linkedMapOf() }
                visible.forEach { item ->
                    val id = item.mediaId ?: return@forEach
                    if (known.size >= 10000 && !known.containsKey(id)) { val oldest = known.keys.first(); known.remove(oldest); knownProofs.remove(oldest) }
                    known[id] = item
                    knownProofs[id] = ItemProof(target, page, fingerprint(item))
                }
                val nextPage = if ((children.size > 25 && (page + 1) * 25 < children.size) || children.size == 25) page + 1 else null
                nativePages.remember(cacheKey, rows, nextPage)
                finish(render(rows, nextPage, false), null)
            }
            override fun onError(parentId: String) = finish(null, "BROWSE_DENIED")
            override fun onError(parentId: String, options: Bundle) = finish(null, "BROWSE_DENIED")
        }
        browseRequests.add(request)
        try { browser.subscribe(target, options, cb) }
        catch (_: Exception) { finish(null, "BROWSE_UNAVAILABLE"); return }
        main.postDelayed({ finish(null, "SOURCE_TIMEOUT") }, 8000)
    }
    fun diagnostic(): JSONObject = JSONObject().put("device", android.os.Build.MODEL).put("os", android.os.Build.VERSION.RELEASE)
        .put("build", android.os.Build.DISPLAY).put("sources", sources()).put("state", state()).put("storage", storageDiagnostic()).put("reportedAudioManager", runCatching {
            val audio = context.getSystemService(android.media.AudioManager::class.java)
            JSONObject().put("musicMin", audio.getStreamMinVolume(android.media.AudioManager.STREAM_MUSIC))
                .put("musicMax", audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)).put("musicCurrent", audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC))
                .put("fixedVolume", audio.isVolumeFixed).put("musicActive", audio.isMusicActive)
                .put("note", "Reported API values only; actual AMP15 output mapping is not verified")
        }.getOrElse { JSONObject().put("error", it.javaClass.simpleName) })
}

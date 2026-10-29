package org.dx340.remote.controller

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.*
import android.os.IBinder
import android.view.WindowInsets
import android.widget.*
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var connection: TextView
    private lateinit var model: RemoteModel
    private var tab = "Player"
    private var bound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            model = (binder as RemoteService.LocalBinder).service.model
            model.observer = { if (::body.isInitialized) render() }; render()
        }
        override fun onServiceDisconnected(name: ComponentName) { connection.text = "Connection service stopped. Reopen the remote." }
    }
    private val parents = mutableListOf<String?>()
    private var navigationScope = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 20, 20, 20) }
        root.setOnApplyWindowInsetsListener { view, insets -> val b = insets.getInsets(WindowInsets.Type.systemBars()); view.setPadding(b.left + 20, b.top + 12, b.right + 20, b.bottom + 12); insets }
        setContentView(root)
        root.addView(TextView(this).apply { text = "DX340 Remote"; textSize = 28f; setAccessibilityHeading(true) })
        connection = TextView(this).apply { textSize = 16f; accessibilityLiveRegion = android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE }; root.addView(connection)
        val nav = LinearLayout(this)
        listOf("Player", "Library", "Queue", "Setup").forEach { name -> nav.addView(Button(this).apply { text = name; setOnClickListener { tab = name; render() } }, LinearLayout.LayoutParams(0, 56.dp(), 1f)) }
        root.addView(nav)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        connection.text = "Preparing Bluetooth remote…"
        bound = bindService(Intent(this, RemoteService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }
    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 18f) { body.addView(TextView(this).apply { text = value; textSize = size; setPadding(0, 12.dp(), 0, 8.dp()) }) }
    private fun button(value: String, enabled: Boolean = true, action: () -> Unit) { body.addView(Button(this).apply { text = value; minHeight = 48.dp(); isEnabled = enabled; setOnClickListener { action() } }) }
    private fun cap(name: String) = model.authorized && model.state.optJSONObject("capabilities")?.optBoolean(name) == true
    private fun render() {
        if (!::model.isInitialized) return
        val scope = model.state.optString("source") + ":" + model.state.optString("sourceEpoch") + ":" + model.state.optJSONObject("capabilities")?.optString("browseMode")
        if (scope != navigationScope) { parents.clear(); navigationScope = scope }
        connection.text = model.status; body.removeAllViews()
        when (tab) {
            "Setup" -> {
                label("Music plays on DX340. Open pairing on the companion before connecting.")
                button("Grant Nearby Devices access") { requestPermissions(if (android.os.Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.POST_NOTIFICATIONS) else arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 340) }
                button("Find DX340") { model.scan() }
                model.found.forEach { device -> button("Connect to DX340 ${device.address.takeLast(5)}") { startForegroundService(Intent(this, RemoteService::class.java)); model.connect(device) } }
                button("Retry authorization after approval") { model.authorize() }
                if (model.canRequestControl) {
                    label("Taking control here disconnects the other approved controller. Music keeps playing on DX340.")
                    button("Take control on this phone") { model.requestControl() }
                }
                button("Disconnect") { model.disconnect(); stopService(Intent(this, RemoteService::class.java)) }
            }
            "Player" -> {
                model.artworkBytes?.let { art -> body.addView(ImageView(this).apply { setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(art, 0, art.size)); contentDescription = "Album artwork"; adjustViewBounds = true; maxHeight = 240.dp() }) }
                label(model.state.optString("title", "No track connected"), 26f)
                label(model.state.optString("artist")); label(model.state.optString("album"))
                button("Play", cap("canPlay")) { model.send("SET_PLAYING", JSONObject().put("playing", true)) }
                button("Pause", cap("canPause")) { model.send("SET_PLAYING", JSONObject().put("playing", false)) }
                button("Previous", cap("canPrevious")) { model.send("PREVIOUS") }; button("Next", cap("canNext")) { model.send("NEXT") }
                label("${model.state.optLong("position") / 1000}s / ${model.state.optLong("duration") / 1000}s")
                button("Back 10 seconds", cap("canSeek")) { model.send("SEEK", JSONObject().put("position", (model.state.optLong("position") - 10000).coerceAtLeast(0))) }
                label("Volume control awaits physical output verification.")
                button("Refresh player", model.authorized) { model.send("GET_STATE") }
                for (i in 0 until model.sources.length()) {
                    val source = model.sources.getJSONObject(i)
                    button("Control ${source.optString("name")}", model.authorized) { parents.clear(); model.send("SELECT_SOURCE", JSONObject().put("source", source.getString("id"))) }
                }
            }
            "Library" -> {
                label("Library", 24f)
                val assisted = model.assistedBrowsing
                if (assisted) label(model.state.optJSONObject("capabilities")?.optString("reason") ?: "Apple Music needs DX340 awake and unlocked.")
                if (!cap("canBrowse") && !assisted) label("This source has not exposed a usable browse connection. Open the source on DX340 and refresh.")
                button("Browse source library", cap("canBrowse")) { parents.clear(); model.browse() }
                if (assisted && !model.library.isNull("backId")) button("Back", cap("canBrowse") && model.assistedReady) { model.browse(model.library.getString("backId")) }
                if (!assisted && parents.isNotEmpty()) button("Back to parent") { parents.removeAt(parents.lastIndex); model.browse(parents.lastOrNull()) }
                if (model.library.optString("title").isNotEmpty()) label(model.library.optString("title"))
                val items = model.library.optJSONArray("items")
                if (items != null) for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    button(item.optString("title"), model.authorized && (!(assisted || item.optBoolean("uiAssisted")) || model.assistedReady) && ((item.optBoolean("browsable") && cap("canBrowse")) || (item.optBoolean("playable") && cap("canPlayItem")))) {
                        val id = item.getString("id")
                        if (item.optBoolean("browsable")) { if (!assisted) parents.add(id); model.browse(id) }
                        else if (item.optBoolean("playable")) model.send("PLAY_ITEM", JSONObject().put("item", id).put("revision", model.library.getString("revision")))
                    }
                }
                if (!model.library.isNull("nextPage")) button("Next page", cap("canBrowse") && (!assisted || model.assistedReady)) { model.browse(model.library.optString("parent"), model.library.getInt("nextPage")) }
            }
            "Queue" -> {
                label("Queue", 24f)
                val revision = model.queueRevision
                button("Refresh queue", cap("canReadQueue") && !model.queueLoading) { model.readQueue() }
                if (model.queueLoading) label("Loading queue…")
                if (model.queuePage.has("total") && model.queue.length() > 0) label("Queue items ${model.queuePage.optInt("offset") + 1}–${model.queuePage.optInt("offset") + model.queue.length()} of ${model.queuePage.optInt("total")}")
                for (i in 0 until model.queue.length()) {
                    val item = model.queue.getJSONObject(i)
                    button(item.optString("title"), cap("canPlayQueueItem") && !model.queueLoading) { model.send("PLAY_QUEUE_ITEM", JSONObject().put("item", item.getString("id")).put("revision", revision)) }
                }
                if (model.queuePage.optInt("offset") > 0) button("Start of queue", cap("canReadQueue") && !model.queueLoading) { model.readQueue() }
                if (!model.queuePage.isNull("nextOffset")) button("Next queue page", cap("canReadQueue") && !model.queueLoading) { model.readQueue(model.queuePage.getInt("nextOffset")) }
                if (!cap("canReadQueue")) label("The source has not exposed a readable queue.")
            }
        }
        if (tab != "Player" && tab != "Setup") label("Now playing: ${model.state.optString("title", "Not connected")}")
    }
    override fun onDestroy() { if (::model.isInitialized) model.observer = null; if (bound) unbindService(serviceConnection); super.onDestroy() }
}

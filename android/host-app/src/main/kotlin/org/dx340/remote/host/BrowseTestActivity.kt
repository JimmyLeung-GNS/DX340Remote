package org.dx340.remote.host

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import org.json.JSONObject
import java.io.File

/** Local test harness. Browsing and confirmed audio results are recorded separately from BLE acceptance. */
class BrowseTestActivity : Activity() {
    private lateinit var body: LinearLayout
    private lateinit var status: TextView
    private lateinit var rows: LinearLayout
    private lateinit var testActions: LinearLayout
    private val parents = mutableListOf<String?>()
    private var page = JSONObject()
    private var selectedItem: JSONObject? = null
    private var selectedRevision: String? = null
    private var requestRunning = false
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val pendingFile get() = File(filesDir, "pending-mango-test.json")
    private val sources get() = HostService.instance?.sources
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        setContentView(ScrollView(this).apply { addView(body); setOnApplyWindowInsetsListener { v, i -> val b = i.getInsets(android.view.WindowInsets.Type.systemBars()); v.setPadding(b.left, b.top, b.right, b.bottom); i } })
        body.addView(TextView(this).apply { text = "Test Mango library"; textSize = 26f; setAccessibilityHeading(true) })
        body.addView(TextView(this).apply { text = "This tests the Mango interface on DX340. Phone Bluetooth control is a separate test. Keep your usual low listening volume; this test does not change it."; textSize = 18f })
        status = TextView(this).apply { textSize = 18f }; body.addView(status)
        testActions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; body.addView(testActions)
        addButton(body, "Back") { if (page.optBoolean("uiAssisted") && !page.isNull("backId")) load(page.getString("backId")) else if (parents.isEmpty()) finish() else { parents.removeAt(parents.lastIndex); load(parents.lastOrNull()) } }
        addButton(body, "Open Mango Player") { packageManager.getLaunchIntentForPackage("com.ibasso.music")?.let { startActivity(it) } }
        addButton(body, "Retry library") { load(parents.lastOrNull(), page.optInt("page", 0)) }
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; body.addView(rows)
        if (sources == null) status.text = "Return to the companion and tap Start Bluetooth remote first. Then open this test again."
        else { sources!!.select("com.ibasso.music"); if (!restorePending()) load(null) }
    }
    private fun addButton(parent: LinearLayout, title: String, action: () -> Unit): Button = Button(this).apply { text = title; minHeight = (48 * resources.displayMetrics.density).toInt(); setOnClickListener { if (!requestRunning) action() } }.also { parent.addView(it) }
    private fun returnFromMango() {
        requestRunning = false
        if (!isFinishing && !isDestroyed) startActivity(Intent(this, BrowseTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }
    private fun withMangoVisible(action: () -> Unit) {
        if (requestRunning) return
        val service = MangoAccessibilityService.instance
        val issue = service?.readiness() ?: if (service == null) "UI_PERMISSION_REQUIRED" else null
        if (issue != null) { status.text = "Mango folders unavailable: $issue. Check optional folder mode and Accessibility access."; return }
        val launch = packageManager.getLaunchIntentForPackage("com.ibasso.music") ?: return
        requestRunning = true
        status.text = "Opening Mango to inspect its current folder…"
        startActivity(launch)
        val deadline = android.os.SystemClock.elapsedRealtime() + 2000
        var visibleSince = 0L
        fun ready() {
            if (isFinishing || isDestroyed) { requestRunning = false; return }
            val now = android.os.SystemClock.elapsedRealtime()
            if (service?.hasForegroundWindow() == true) {
                if (visibleSince == 0L) visibleSince = now
                if (now - visibleSince >= 350) {
                    try { action() } catch (e: Exception) { status.text = "Mango test interrupted: ${e.message}. Retry the library."; returnFromMango() }
                    return
                }
            } else visibleSince = 0L
            if (now >= deadline) { status.text = "Could not open Mango. Open it once, return and retry."; returnFromMango(); return }
            main.postDelayed({ ready() }, 100)
        }
        main.postDelayed({ ready() }, 100)
    }
    private fun load(parent: String?, number: Int = 0) {
        status.text = "Loading Mango library…"; rows.removeAllViews(); testActions.removeAllViews(); selectedItem = null; pendingFile.delete()
        val expected = if (parent?.startsWith("mango-ui:") == true && parent != org.dx340.remote.sources.SourceRegistry.MANGO_UI_ROOT) page.optString("revision") else null
        val assistedRequest = parent?.startsWith("mango-ui:") == true && parent != org.dx340.remote.sources.SourceRegistry.MANGO_UI_ROOT
        val request = { sources?.browse(parent, number, expected) { data, error ->
            if (assistedRequest) returnFromMango()
            if (isFinishing || isDestroyed) return@browse
            if (error != null || data == null) { status.text = "Mango returned ${error ?: "no library"}. Open Mango once, return here and retry."; return@browse }
            page = data
            status.text = if (data.optBoolean("uiAssisted")) "${data.optString("title", "Mango folders")}\nOptional folder mode needs DX340 awake/unlocked. Choose a folder or file to test." else "Choose a folder or a track to test."
            val items = data.getJSONArray("items")
            if (data.optBoolean("uiAssisted") && items.length() == 0) status.append("\nNo local items are shown here. Use Back to choose another folder.")
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                addButton(rows, item.optString("title")) {
                    if (item.optBoolean("browsable")) { if (!data.optBoolean("uiAssisted") && !item.optBoolean("uiAssisted")) parents.add(item.getString("id")); load(item.getString("id")) }
                    else if (item.optBoolean("playable") || item.optBoolean("nativeTestable")) confirmTest(item)
                }.apply { isEnabled = item.optBoolean("browsable") || item.optBoolean("playable") || item.optBoolean("nativeTestable"); if (data.optBoolean("uiAssisted")) isAllCaps = false }
            }
            if (!data.isNull("nextPage")) addButton(rows, "Next page") { load(data.getString("parent"), data.getInt("nextPage")) }
        }; Unit }
        try { if (assistedRequest) withMangoVisible(request) else request() } catch (e: Exception) { if (assistedRequest) returnFromMango(); status.text = e.message }
    }
    private fun confirmTest(item: JSONObject) {
        if (page.optBoolean("uiAssisted")) {
            AlertDialog.Builder(this).setTitle("Test this file in Mango?")
                .setMessage("${item.optString("title")}\n\nThis optional mode clicks the current local file in Mango. DX340 must stay awake/unlocked. Listen and confirm the correct track; this does not verify phone Bluetooth or native play-by-ID.")
                .setNegativeButton("Cancel", null).setPositiveButton("Test selected file") { _, _ ->
                    val source = sources ?: return@setPositiveButton
                    val epoch = source.epoch("com.ibasso.music")
                    withMangoVisible { source.playItem(JSONObject().put("item", item.getString("id")).put("revision", page.getString("revision")), { !isFinishing && !isDestroyed }) { error ->
                        returnFromMango()
                        if (isFinishing || isDestroyed) return@playItem
                        if (error != null) { status.text = "Mango folder test failed: $error"; return@playItem }
                        selectedItem = JSONObject(item.toString()).put("testRoute", "ui-assisted"); selectedRevision = epoch
                        pendingFile.writeText(JSONObject().put("item", selectedItem).put("revision", epoch).put("page", page).put("parents", org.json.JSONArray(parents)).toString())
                        showPendingActions()
                    } }
                }.show()
            return
        }
        AlertDialog.Builder(this).setTitle("Test this track in Mango?")
            .setMessage("${item.optString("title")}\n\nMango exposes this playable item but does not advertise play-by-ID in its action flags. This experiment calls the public API. Check the DX340 display and listen before confirming success.")
            .setNegativeButton("Cancel", null).setPositiveButton("Test selected track") { _, _ ->
                try {
                    val revision = page.getString("revision")
                    sources!!.experimentMangoPlayItem(item.getString("id"), revision)
                    selectedItem = item; selectedRevision = revision
                    pendingFile.writeText(JSONObject().put("item", item).put("revision", revision).put("page", page).put("parents", org.json.JSONArray(parents)).toString())
                    showPendingActions()
                } catch (e: Exception) { status.text = e.message ?: "Test failed" }
            }.show()
    }
    private fun showPendingActions() {
        status.text = "Requested: ${selectedItem?.optString("title")}\nOpen Mango to inspect the track, then return and record what you heard."
        testActions.removeAllViews()
        addButton(testActions, "Correct selected track is audible") { record(true) }
        addButton(testActions, "Wrong track, silent or failed") { record(false) }
        testActions.requestFocus()
    }
    private fun restorePending(): Boolean = try {
        if (!pendingFile.isFile || pendingFile.length() > 65536) false else {
            val saved = JSONObject(pendingFile.readText())
            if (saved.getString("revision") != sources!!.epoch("com.ibasso.music")) { pendingFile.delete(); false } else {
                selectedItem = saved.getJSONObject("item"); selectedRevision = saved.getString("revision"); page = saved.getJSONObject("page")
                val path = saved.optJSONArray("parents")
                if (path != null) for (i in 0 until path.length()) parents.add(if (path.isNull(i)) null else path.getString(i))
                showPendingActions(); true
            }
        }
    } catch (_: Exception) { pendingFile.delete(); false }
    private fun record(success: Boolean) {
        val source = sources ?: return
        val selected = selectedItem ?: return
        val assisted = selected.optString("testRoute") == "ui-assisted"
        if (source.selected != "com.ibasso.music" || source.epoch(source.selected) != selectedRevision) { status.text = "Source changed; repeat the test before confirming."; return }
        try {
        val event = JSONObject().put("time", System.currentTimeMillis()).put("device", android.os.Build.MODEL).put("build", android.os.Build.DISPLAY)
            .put("source", "com.ibasso.music").put("version", packageManager.getPackageInfo("com.ibasso.music", 0).versionName)
            .put("sourceVersionCode", packageManager.getPackageInfo("com.ibasso.music", 0).longVersionCode).put("firmwareFingerprint", android.os.Build.FINGERPRINT)
            .put("output", "AMP15 phone out (user reported)").put("requested", selected).put("observed", source.state())
            .put("result", if (success) "USER_CONFIRMED_AUDIBLE" else "USER_REPORTED_FAILED").put("path", if (assisted) "local own Mango AccessibilityService file selection; BLE not involved" else "local public playFromMediaId experiment; BLE not involved")
        java.io.FileOutputStream(File(filesDir, if (assisted) "mango-ui-playback-tests.jsonl" else "mango-playback-tests.jsonl"), true).use { it.write((event.toString() + "\n").toByteArray()); it.fd.sync() }
        if (!assisted) source.confirmMangoPlayItem(success)
        pendingFile.delete()
        selectedItem = null; selectedRevision = null
        status.text = if (assisted) "${if (success) "Audible file selection" else "Failed file selection"} recorded for optional Mango folder mode. This does not enable or validate native play-by-ID, phone Bluetooth or the full format matrix." else if (success) "Audible success recorded for this Mango version. Play-by-ID is now enabled for remote tests; full 20-track acceptance remains outstanding." else "Failure recorded. Mango remains the playback engine; arbitrary remote selection is still unverified."
        testActions.removeAllViews()
        } catch (_: Exception) { status.text = "Could not save the result or source verification. Free storage and retry recording; no success has been confirmed on this screen." }
    }
}

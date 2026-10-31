package org.dx340.remote.host

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var connection: TextView
    private lateinit var remoteAction: Button
    private lateinit var pairing: Button
    private lateinit var setupSummary: TextView
    private lateinit var setupActions: LinearLayout
    private lateinit var setupToggle: Button
    private lateinit var browsingSummary: TextView
    private lateinit var browsingActions: LinearLayout
    private lateinit var browsingToggle: Button
    private lateinit var technical: TextView
    private lateinit var appleMode: Button
    private lateinit var mangoMode: Button
    private lateinit var controllers: ControllerManagementPanel
    private var showSetup = false
    private var showBrowsing = false
    private var showAdvanced = false
    private var showDiagnostics = false
    private var permissionAction: String? = null
    private val pairingExpiry = Runnable { refresh() }
    private val permissions get() = mutableListOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE).apply { if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSetup = savedInstanceState?.getBoolean("showSetup") ?: false
        showBrowsing = savedInstanceState?.getBoolean("showBrowsing") ?: false
        showAdvanced = savedInstanceState?.getBoolean("showAdvanced") ?: false
        showDiagnostics = savedInstanceState?.getBoolean("showDiagnostics") ?: false
        permissionAction = savedInstanceState?.getString("permissionAction")
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(24)) }
        setContentView(ScrollView(this).apply { addView(content); setOnApplyWindowInsetsListener { view, insets -> val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets } })
        label("DX340 Companion", 28f, true)
        label("Music plays here. Your phone controls it over Bluetooth.")
        section("Connection")
        connection = label("").apply { minLines = 2; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        remoteAction = button("Enable phone control") {
            start(if (ControllerApprovalStore(this).identities().isEmpty()) "PAIR" else "START")
        }
        pairing = button("Set up a new phone") { start("PAIR") }
        controllers = ControllerManagementPanel(this, ::refresh)
        content.addView(controllers, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        section("Setup access")
        setupSummary = label("")
        setupToggle = button("Review permissions and access") { showSetup = !showSetup; refresh() }
        setupActions = group()
        button("Allow Bluetooth and notifications", setupActions) { requestPermissions(permissions, 340) }
        button("Open Bluetooth settings", setupActions) { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        button("Open playback access settings", setupActions) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        label("Playback access lets the companion read and control the existing music apps. Notifications keep the remote service visible.", parent = setupActions)

        section("Music browsing")
        browsingSummary = label("")
        browsingToggle = button("Review browsing and local checks") { showBrowsing = !showBrowsing; refresh() }
        browsingActions = group()
        label("Native Mango albums and playback controls are separate from optional screen-assisted browsing.", parent = browsingActions)
        mangoMode = button("Enable optional Mango browsing", browsingActions) {
            toggleBrowsing("mango-ui")
        }
        label("Mango folders and existing playlists need DX340 awake/unlocked and Accessibility access. Mango updates require revalidation.", parent = browsingActions)
        appleMode = button("Enable optional Apple Music browsing", browsingActions) {
            toggleBrowsing("apple-ui")
        }
        label("Downloaded browsing uses Apple Music's screens and needs DX340 awake/unlocked. Downloads stay managed by Apple Music.", parent = browsingActions)
        button("Open browsing access settings", browsingActions) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        button("Open Apple Music", browsingActions) { packageManager.getLaunchIntentForPackage("com.apple.android.music")?.let { startActivity(it) } }
        button("Test Mango library on this DX340", browsingActions) { startActivity(Intent(this, BrowseTestActivity::class.java)) }
        button("Verify output volume on this DX340", browsingActions) { startActivity(Intent(this, VolumeCheckActivity::class.java)) }
        label("These local checks use this DX340. Test Bluetooth control separately from the phone, without changing its listening volume.", parent = browsingActions)

        section("Advanced")
        lateinit var advanced: LinearLayout
        val advancedToggle = button("Show advanced controls") { showAdvanced = !showAdvanced; advanced.visibility = if (showAdvanced) View.VISIBLE else View.GONE; (it as Button).text = if (showAdvanced) "Hide advanced controls" else "Show advanced controls" }
        advanced = group()
        button("Stop Bluetooth remote", advanced) {
            HostControlStore(this).setEnabled(false)
            stopService(Intent(this, HostService::class.java))
            content.postDelayed({ attach(); refresh() }, 500)
        }
        button("Probe source capabilities", advanced) { start("PROBE") }
        button("Show / hide technical diagnostics", advanced) { showDiagnostics = !showDiagnostics; refresh() }
        button("Export diagnostics", advanced) {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE, "dx340-diagnostic.json").addCategory(Intent.CATEGORY_OPENABLE), 341)
        }
        button("Revoke all controller approvals", advanced) {
            val service = HostService.instance
            if (service != null) service.revokeAll() else ControllerApprovalStore(this).revokeAll()
            refresh()
        }
        technical = label("", 16f, parent = advanced).apply { setTextIsSelectable(true) }
        advanced.visibility = if (showAdvanced) View.VISIBLE else View.GONE
        advancedToggle.text = if (showAdvanced) "Hide advanced controls" else "Show advanced controls"
        refresh()
        if (intent.getBooleanExtra("developmentProbe", false) && applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) start("PROBE")
    }
    private fun label(text: String, size: Float = 16f, heading: Boolean = false, parent: LinearLayout = content): TextView = TextView(this).apply {
        this.text = text; textSize = size; useCompanionPrimaryText(); if (heading) setAccessibilityHeading(true)
    }.also { parent.addView(it) }
    private fun section(text: String) = label(text, 22f, true).apply {
        layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { topMargin = dp(24); bottomMargin = dp(8) }
    }
    private fun group() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }.also { content.addView(it) }
    private fun button(label: String, parent: LinearLayout = content, action: (View) -> Unit): Button = Button(this).apply {
        text = label; minHeight = dp(48); setOnClickListener(action)
    }.also { parent.addView(it) }
    private fun toggleBrowsing(name: String) {
        val prefs = getSharedPreferences(name, MODE_PRIVATE)
        val enable = !prefs.getBoolean("enabled", false)
        prefs.edit().putBoolean("enabled", enable).apply()
        HostService.instance?.sources?.externalStateChanged(); refresh()
        if (enable) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    private fun start(action: String) {
        if (permissions.take(2).any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) { permissionAction = action; requestPermissions(permissions, 340); return }
        startForegroundService(Intent(this, HostService::class.java).setAction(action))
        content.postDelayed({ attach(); refresh() }, 500)
    }
    private fun attach() { HostService.instance?.observer = { runOnUiThread { refresh() } } }
    @SuppressLint("MissingPermission")
    private fun refresh() {
        val service = HostService.instance
        val bluetoothAccess = permissions.take(2).all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        val bluetoothOn = bluetoothAccess && runCatching { getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true }.getOrDefault(false)
        val playbackAccess = getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(ComponentName(this, SessionListener::class.java))
        val notifications = getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        val remaining = service?.pairingRemainingMs ?: 0L
        connection.text = when {
            !bluetoothAccess -> "Allow Bluetooth access below, then start the remote."
            !bluetoothOn -> "Bluetooth is off. Open Bluetooth settings below to enable it."
            service == null -> "Enable phone control here, then tap Connect on your phone."
            service.activeControllerAddress != null -> "Controller connected. Music stays on DX340."
            service.status.startsWith("BLE_START_FAILED") || service.status.startsWith("ADVERTISING_FAILED") || service.status.startsWith("GATT_SERVICE_FAILED") || service.status.startsWith("BLE_REPLY_TIMEOUT") -> "Bluetooth setup failed. Stop and start the remote, then reconnect your phone."
            service.pending != null -> "A phone is requesting access. Review it below."
            remaining > 0 -> "On your phone, tap Connect. Confirm matching system codes on both devices, then approve the phone below."
            else -> "Ready for your phone. Tap Connect in the phone app; approved phones need no new pairing."
        }
        remoteAction.visibility = if (service == null) View.VISIBLE else View.GONE
        remoteAction.text = if (ControllerApprovalStore(this).identities().isEmpty()) "Set up first phone" else "Enable phone control"
        remoteAction.isEnabled = !bluetoothAccess || bluetoothOn
        pairing.visibility = if (service != null) View.VISIBLE else View.GONE
        pairing.isEnabled = bluetoothAccess && bluetoothOn && remaining <= 0
        pairing.text = if (remaining > 0) "New phone setup open · ${(remaining + 999) / 1000}s" else "Set up a new phone"
        setupSummary.text = "Bluetooth: ${if (bluetoothAccess) "allowed" else "access needed"} · Playback access: ${if (playbackAccess) "on" else "needed"}\nService notifications: ${if (notifications) "on" else "off"}"
        setupActions.visibility = if (showSetup) View.VISIBLE else View.GONE
        setupToggle.text = if (showSetup) "Hide permissions and access" else "Review permissions and access"
        val mangoEnabled = getSharedPreferences("mango-ui", MODE_PRIVATE).getBoolean("enabled", false)
        val appleEnabled = getSharedPreferences("apple-ui", MODE_PRIVATE).getBoolean("enabled", false)
        val accessServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            .split(':').mapNotNull(ComponentName::unflattenFromString).toSet()
        fun browsingState(enabled: Boolean, component: Class<*>, bound: Boolean) = when {
            !enabled -> "off"
            ComponentName(this, component) !in accessServices -> "on · Accessibility access needed"
            !bound -> "on · access not ready"
            else -> "on"
        }
        browsingSummary.text = "Mango folders/playlists: ${browsingState(mangoEnabled, MangoAccessibilityService::class.java, MangoAccessibilityService.instance != null)}\nApple Music downloads: ${browsingState(appleEnabled, AppleMusicAccessibilityService::class.java, AppleMusicAccessibilityService.instance != null)}"
        browsingActions.visibility = if (showBrowsing) View.VISIBLE else View.GONE
        browsingToggle.text = if (showBrowsing) "Hide browsing and local checks" else "Review browsing and local checks"
        appleMode.text = if (appleEnabled) "Disable optional Apple Music browsing" else "Enable optional Apple Music browsing"
        mangoMode.text = if (mangoEnabled) "Disable optional Mango browsing" else "Enable optional Mango browsing"
        controllers.render(service)
        content.removeCallbacks(pairingExpiry)
        if (remaining > 0) content.postDelayed(pairingExpiry, minOf(1000L, remaining + 25))
        technical.visibility = if (showDiagnostics) View.VISIBLE else View.GONE
        if (showDiagnostics) technical.text = if (service == null) "Remote stopped." else service.status + "\n\n" + service.sources.diagnostic().toString(2)
    }
    override fun onResume() { super.onResume(); matchCompanionStatusBar(); attach(); refresh() }
    override fun onPause() { HostService.instance?.observer = null; content.removeCallbacks(pairingExpiry); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("showSetup", showSetup); outState.putBoolean("showBrowsing", showBrowsing); outState.putBoolean("showAdvanced", showAdvanced); outState.putBoolean("showDiagnostics", showDiagnostics)
        outState.putString("permissionAction", permissionAction)
        super.onSaveInstanceState(outState)
    }
    @Deprecated("Activity permission result callback for the dependency-free companion")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 340) {
            val action = permissionAction; permissionAction = null
            if (action != null && this.permissions.take(2).all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) start(action)
        }
        refresh()
    }
    @Deprecated("Activity result API for dependency-free diagnostic app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 341 && resultCode == RESULT_OK) data?.data?.let { uri -> contentResolver.openOutputStream(uri)?.use { it.write((HostService.instance?.diagnostic() ?: "{\"status\":\"stopped\"}").toByteArray()) } }
    }
}

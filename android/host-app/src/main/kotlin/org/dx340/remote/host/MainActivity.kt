package org.dx340.remote.host

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private var showDiagnostics = false
    private lateinit var appleMode: Button
    private lateinit var mangoMode: Button
    private lateinit var controllers: ControllerManagementPanel
    private val pairingExpiry = Runnable { refresh() }
    private val permissions get() = mutableListOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE).apply { if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        setContentView(ScrollView(this).apply { addView(content); setOnApplyWindowInsetsListener { view, insets -> val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets } })
        content.addView(TextView(this).apply { text = "DX340 Companion"; textSize = 28f; setAccessibilityHeading(true) })
        content.addView(TextView(this).apply { text = "Music stays in Mango Player or Apple Music. Enable the remote, then authorize your phone here."; textSize = 18f })
        button("Start Bluetooth remote") { start("START") }
        button("Test Mango library on this DX340") { startActivity(Intent(this, BrowseTestActivity::class.java)) }
        button("Verify output volume on this DX340") { startActivity(Intent(this, VolumeCheckActivity::class.java)) }
        appleMode = button("Enable optional Apple Music browsing") {
            val prefs = getSharedPreferences("apple-ui", MODE_PRIVATE)
            val enable = !prefs.getBoolean("enabled", false)
            prefs.edit().putBoolean("enabled", enable).apply()
            HostService.instance?.sources?.externalStateChanged(); refresh()
            if (enable) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        button("Open Apple Music on this DX340") { packageManager.getLaunchIntentForPackage("com.apple.android.music")?.let { startActivity(it) } }
        mangoMode = button("Enable optional Mango browsing") {
            val prefs = getSharedPreferences("mango-ui", MODE_PRIVATE)
            val enable = !prefs.getBoolean("enabled", false)
            prefs.edit().putBoolean("enabled", enable).apply()
            HostService.instance?.sources?.externalStateChanged(); refresh()
            if (enable) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        content.addView(TextView(this).apply { text = "Optional browsing opens folders and existing playlists through Mango. It needs DX340 awake/unlocked and Accessibility access; Mango updates require revalidation. Native albums and controls remain available."; textSize = 18f })
        button("Grant Bluetooth permissions") { requestPermissions(permissions, 340) }
        button("Enable playback session access") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        button("Open pairing for 60 seconds") { start("PAIR") }
        controllers = ControllerManagementPanel(this, ::refresh)
        content.addView(controllers)
        button("Probe source capabilities") { start("PROBE") }
        button("Show / hide technical diagnostics") { showDiagnostics = !showDiagnostics; refresh() }
        button("Export diagnostics") {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE, "dx340-diagnostic.json").addCategory(Intent.CATEGORY_OPENABLE), 341)
        }
        button("Revoke all controller approvals") {
            val service = HostService.instance
            if (service != null) service.revokeAll() else ControllerApprovalStore(this).revokeAll()
            refresh()
        }
        button("Stop Bluetooth remote") { stopService(Intent(this, HostService::class.java)); refresh() }
        status = TextView(this).apply { textSize = 16f; setTextIsSelectable(true) }; content.addView(status)
        refresh()
        if (intent.getBooleanExtra("developmentProbe", false) && applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) start("PROBE")
    }
    private fun button(label: String, action: () -> Unit): Button = Button(this).apply { text = label; minHeight = (48 * resources.displayMetrics.density).toInt(); setOnClickListener { action() } }.also { content.addView(it) }
    private fun start(action: String) {
        if (permissions.take(2).any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) { requestPermissions(permissions, 340); return }
        startForegroundService(Intent(this, HostService::class.java).setAction(action))
        content.postDelayed({ attach(); refresh() }, 500)
    }
    private fun attach() { HostService.instance?.observer = { runOnUiThread { refresh() } } }
    private fun refresh() {
        if (::appleMode.isInitialized) appleMode.text = if (getSharedPreferences("apple-ui", MODE_PRIVATE).getBoolean("enabled", false)) "Disable optional Apple Music browsing" else "Enable optional Apple Music browsing"
        if (::mangoMode.isInitialized) mangoMode.text = if (getSharedPreferences("mango-ui", MODE_PRIVATE).getBoolean("enabled", false)) "Disable optional Mango browsing" else "Enable optional Mango browsing"
        val service = HostService.instance
        if (::controllers.isInitialized) controllers.render(service)
        content.removeCallbacks(pairingExpiry)
        if (service != null && service.pairingRemainingMs > 0) content.postDelayed(pairingExpiry, service.pairingRemainingMs + 25)
        status.text = if (service == null) "Remote stopped. Tap Start Bluetooth remote, then Test Mango library to try browsing on this DX340." else if (showDiagnostics) service.status + "\n\n" + service.sources.diagnostic().toString(2) else "${service.status}\n\nNo controller phone is required for the local Mango library test. A phone running DX340 Remote is required to test Bluetooth control.\n\nApple Music library access is still under investigation."
    }
    override fun onResume() { super.onResume(); attach(); refresh() }
    override fun onPause() { HostService.instance?.observer = null; content.removeCallbacks(pairingExpiry); super.onPause() }
    @Deprecated("Activity result API for dependency-free diagnostic app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 341 && resultCode == RESULT_OK) data?.data?.let { uri -> contentResolver.openOutputStream(uri)?.use { it.write((HostService.instance?.diagnostic() ?: "{\"status\":\"stopped\"}").toByteArray()) } }
    }
}

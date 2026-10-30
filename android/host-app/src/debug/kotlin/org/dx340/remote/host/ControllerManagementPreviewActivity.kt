package org.dx340.remote.host

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.UUID

/** Debug-only UI fixture. Its store and host provider cannot modify live approvals. */
class ControllerManagementPreviewActivity : Activity() {
    private val preferencesName = "controller-preview-${UUID.randomUUID()}"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ControllerApprovalStore(this, preferencesName)
        store.approve("AA:00:00:00:00:01"); store.approve("AA:00:00:00:00:02")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val density = resources.displayMetrics.density
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (24 * density).toInt())
        }
        layout.addView(TextView(this).apply { text = "Development preview"; textSize = 28f; useCompanionPrimaryText(); setAccessibilityHeading(true) })
        layout.addView(TextView(this).apply { text = "Synthetic controller states. No OS bond or live controller is changed."; textSize = 16f; useCompanionPrimaryText() })
        val scenario = intent.getStringExtra("scenario") ?: "saved"
        val approved = listOf(ApprovedControllerState("AA:00:00:00:00:01", "Example phone · 00:00:01", "Paired"),
            ApprovedControllerState("AA:00:00:00:00:02", "Example laptop · 00:00:02", "Paired"))
        val pending = PendingControllerState("AA:00:00:00:00:03", "New example phone · 00:00:03", scenario != "unpaired")
        var state = ControllerAccessState(approved, if (scenario in listOf("paired", "unpaired", "expired", "ended", "approved")) pending else null,
            scenario != "expired", "preview-host")
        lateinit var panel: ControllerManagementPanel
        panel = ControllerManagementPanel(this, { panel.renderSnapshot(state) }, store, { null })
        layout.addView(panel)
        setContentView(ScrollView(this).apply {
            addView(layout)
            setOnApplyWindowInsetsListener { view, insets -> val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets }
        })
        panel.renderSnapshot(state)
        if (scenario == "ended" || scenario == "approved") {
            state = state.copy(pending = null, approved = if (scenario == "approved") approved + ApprovedControllerState(pending.address, pending.label, "Paired") else approved)
            panel.renderSnapshot(state)
        }
    }
    override fun onResume() { super.onResume(); matchCompanionStatusBar() }
    override fun onDestroy() { deleteSharedPreferences(preferencesName); super.onDestroy() }
}

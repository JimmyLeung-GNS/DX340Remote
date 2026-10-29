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
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 64, 24, 24) }
        layout.addView(TextView(this).apply { text = "Development preview: synthetic approvals only. No OS bond or live controller is changed."; textSize = 18f })
        lateinit var panel: ControllerManagementPanel
        panel = ControllerManagementPanel(this, { panel.render(null) }, store, { null })
        layout.addView(panel)
        setContentView(ScrollView(this).apply { addView(layout) })
        panel.render(null)
    }
    override fun onDestroy() { deleteSharedPreferences(preferencesName); super.onDestroy() }
}

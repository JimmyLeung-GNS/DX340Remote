package org.dx340.remote.probe

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Separate opt-in research app. No BLE route and no performAction calls. */
class AppleUiProbeService : AccessibilityService() {
    private var lastCapture = 0L
    override fun onInterrupt() = Unit
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastCapture < 1000) return
        lastCapture = now
        val report = JSONObject().put("time", System.currentTimeMillis()).put("device", android.os.Build.MODEL)
            .put("build", android.os.Build.DISPLAY).put("mode", "debug-read-only-accessibility-experiment")
        val unlocked = !getSystemService(KeyguardManager::class.java).isDeviceLocked
        val interactive = getSystemService(PowerManager::class.java).isInteractive
        report.put("screenOn", interactive).put("unlocked", unlocked)
        if (!unlocked || !interactive) { write(report.put("result", "BLOCKED_SCREEN_OFF_OR_LOCKED")); return }
        val root = rootInActiveWindow
        if (root?.packageName?.toString() != "com.apple.android.music") { write(report.put("result", "BLOCKED_UNEXPECTED_APP")); return }
        val nodes = JSONArray()
        val pending = ArrayDeque<Pair<AccessibilityNodeInfo, String>>(); pending.add(root to "0")
        var inspected = 0
        while (pending.isNotEmpty() && inspected++ < 300) {
            val (node, path) = pending.removeFirst()
            if (node.packageName?.toString() != "com.apple.android.music" || node.isPassword || node.isEditable) continue
            val label = node.text?.toString()?.take(160) ?: node.contentDescription?.toString()?.take(160) ?: ""
            nodes.put(JSONObject().put("path", path).put("label", label).put("viewId", node.viewIdResourceName ?: "").put("clickable", node.isClickable))
            for (i in 0 until minOf(node.childCount, 50)) node.getChild(i)?.let { pending.add(it to "$path.$i") }
        }
        report.put("result", "CAPTURED_READ_ONLY_NO_PLAYBACK_PROOF").put("nodes", nodes)
        write(report)
    }
    private fun write(report: JSONObject) { File(filesDir, "apple-ui-probe.json").writeText(report.toString(2)) }
}

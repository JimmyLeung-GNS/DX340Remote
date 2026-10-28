package org.dx340.remote.host

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*

/** Local listening confirmations only. A service restart deliberately loses verification. */
class VolumeCheckActivity : Activity() {
    private lateinit var body: LinearLayout
    private lateinit var summary: TextView
    private lateinit var message: TextView
    private lateinit var output: EditText
    private lateinit var begin: Button
    private lateinit var heardDown: Button
    private lateinit var returnStep: Button
    private lateinit var heardUp: Button
    private lateinit var cancel: Button
    private lateinit var sources: List<Button>
    private var displayedVolume = -1
    private var displayedCheck = ""
    private val control get() = HostService.instance?.sources?.outputVolume?.control
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Verify output volume"
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        setContentView(ScrollView(this).apply { addView(body); setOnApplyWindowInsetsListener { view, insets -> val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets } })
        text("Verify output volume", 28f).setAccessibilityHeading(true)
        text("Use your usual low listening level. Play the chosen source, then listen to one step down and a return to the starting level. Only those two levels become available on the phone.")
        summary = text("")
        sources = listOf("Mango Player" to "com.ibasso.music", "Apple Music" to "com.apple.android.music").map { (name, pkg) ->
            button("Choose $name") { HostService.instance?.sources?.select(pkg); refresh() }
        }
        text("Output configuration", 20f).setAccessibilityHeading(true)
        output = EditText(this).apply { hint = "Amp and output, for example AMP15 phone out"; maxLines = 2; setSingleLine(false); setText(savedInstanceState?.getString("outputLabel") ?: "") }
        body.addView(output)
        text("Verification lasts for this companion session. Recheck after changing amp, output, headphones or gain; Android cannot identify every amp setting.")
        begin = button("Test one step quieter") { run { control?.begin(output.text.toString(), displayedVolume, ::complete); refresh() } }
        heardDown = button("I heard the quieter step") { run { control?.confirmDown(displayedCheck); refresh() } }
        returnStep = button("Return to starting volume") { run { control?.returnStep(displayedCheck, ::complete); refresh() } }
        heardUp = button("I heard the return · enable this range") { run { control?.confirmReturn(displayedCheck); refresh() } }
        cancel = button("Clear verification / cancel check") { run { control?.reset(); message.text = "Verification cleared. The volume stays at its current level; use the wheel if you want to change it."; refresh() } }
        message = text("")
        button("Refresh output status") { refresh() }
        button("Back to companion") { finish() }
        refresh()
    }
    private fun text(value: String, size: Float = 18f) = TextView(this).apply { text = value; textSize = size; setPadding(0, 8, 0, 8) }.also { body.addView(it) }
    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value; minHeight = (48 * resources.displayMetrics.density).toInt(); setOnClickListener { action() }
    }.also { body.addView(it) }
    private fun run(action: () -> Unit) { try { message.text = ""; action() } catch (e: Exception) { complete(e.message) } }
    private fun complete(error: String?) { message.text = if (error == null) "Output readback received. Confirm only what you heard." else "Check interrupted ($error). Refresh; no automatic volume retry or restoration was sent."; refresh() }
    private fun refresh() {
        val registry = HostService.instance?.sources
        val data = registry?.outputVolume?.state()
        val check = control?.check
        val busy = control?.busy == true
        displayedVolume = data?.optInt("current", -1) ?: -1; displayedCheck = check?.id ?: ""
        summary.text = if (registry == null) "Start Bluetooth remote in the companion first." else
            "${if (registry.selected == "com.ibasso.music") "Mango Player" else "Apple Music"} · volume $displayedVolume\n" +
                (if (data?.optBoolean("verified") == true) "Confirmed range ${data.optInt("verifiedMinimum")}–${data.optInt("verifiedMaximum")} · ${data.optString("outputLabel")}"
                else if (check != null) when (check.phase) { "down" -> "Listen: did the one-step decrease sound quieter?"; "return" -> "Return only to the level at the start of this check."; else -> "Listen: did the return sound louder?" }
                else data?.optString("reason") ?: "Output unavailable.")
        sources.forEach { it.isEnabled = !busy && check == null }
        output.isEnabled = !busy && check == null
        begin.visibility = if (check == null) View.VISIBLE else View.GONE
        begin.isEnabled = registry != null && !busy && data?.optBoolean("verified") != true
        heardDown.visibility = if (check?.phase == "down") View.VISIBLE else View.GONE
        returnStep.visibility = if (check?.phase == "return") View.VISIBLE else View.GONE
        heardUp.visibility = if (check?.phase == "up") View.VISIBLE else View.GONE
        listOf(heardDown, returnStep, heardUp, cancel).forEach { it.isEnabled = !busy && registry != null }
    }
    override fun onResume() { super.onResume(); HostService.instance?.observer = { runOnUiThread { refresh() } }; refresh() }
    override fun onPause() { HostService.instance?.observer = null; super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("outputLabel", output.text.toString()); super.onSaveInstanceState(outState) }
}

package org.dx340.remote.host

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

internal data class ApprovedControllerState(val address: String, val label: String, val connection: String)
internal data class PendingControllerState(val address: String, val label: String, val bonded: Boolean)
internal data class ControllerAccessState(val approved: List<ApprovedControllerState>, val pending: PendingControllerState?, val windowOpen: Boolean, val hostEpoch: String?)

/** Local owner controls only; never exposed as a command on the BLE protocol. */
@SuppressLint("MissingPermission")
class ControllerManagementPanel(context: Context, private val changed: () -> Unit,
    private val store: ControllerApprovalStore = ControllerApprovalStore(context),
    private val runningService: () -> HostService? = { HostService.instance }) : LinearLayout(context) {
    private var rendered = ""
    private var showApproved = false
    private var previous: ControllerAccessState? = null
    private var requestNotice: String? = null
    init { orientation = VERTICAL }
    private fun caption(text: String, heading: Boolean = false) {
        addView(TextView(context).apply { this.text = text; textSize = if (heading) 22f else 16f; if (heading) setAccessibilityHeading(true) })
    }
    private fun action(text: String, enabled: Boolean = true, run: () -> Unit) {
        addView(Button(context).apply { this.text = text; isEnabled = enabled; minHeight = (48 * resources.displayMetrics.density).toInt(); setOnClickListener { run(); changed() } })
    }
    private fun label(address: String, device: BluetoothDevice?): String {
        val name = runCatching { device?.name }.getOrNull()?.replace(Regex("[\\p{Cntrl}\u202A-\u202E\u2066-\u2069]"), "")?.trim()?.take(60)?.takeIf { it.isNotEmpty() } ?: "Controller"
        return "$name · ${address.takeLast(8)}"
    }
    fun render(service: HostService?) {
        val ids = store.identities().sorted()
        val canReadBluetooth = context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val bonded = if (canReadBluetooth) runCatching {
            context.getSystemService(BluetoothManager::class.java).adapter?.takeIf { it.isEnabled }?.bondedDevices?.associateBy { it.address }
        }.getOrNull() else null
        val pending = service?.pending?.takeIf { canReadBluetooth }
        val pendingBonded = canReadBluetooth && runCatching { pending?.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
        val labels = ids.associateWith { label(it, bonded?.get(it)) }
        val current = service?.activeControllerAddress
        val approved = ids.map { address ->
            val display = labels.getValue(address).let { if (labels.values.count { value -> value == it } > 1) "$it · $address" else it }
            val connection = when { current == address -> "Current controller"; bonded == null -> "Bluetooth status unavailable"; address in bonded -> "Paired"; else -> "Pair again to reconnect" }
            ApprovedControllerState(address, display, connection)
        }
        renderSnapshot(ControllerAccessState(approved,
            pending?.let { PendingControllerState(it.address, label(it.address, if (canReadBluetooth) it else null), pendingBonded) },
            service != null && service.pairingRemainingMs > 0, service?.epoch))
    }
    /** Also used by the isolated debug fixture, whose service provider cannot approve live devices. */
    internal fun renderSnapshot(state: ControllerAccessState) {
        val oldPending = previous?.pending
        if (oldPending != null && state.pending == null) requestNotice =
            if (state.approved.any { it.address == oldPending.address }) "Phone approved. On that phone, retry authorization."
            else "The previous request is no longer waiting. Find and connect to DX340 again from the phone."
        if (state.pending != null || state.approved.any { it.connection == "Current controller" }) requestNotice = null
        previous = state
        val key = "$state:$showApproved:$requestNotice"
        if (key == rendered) return
        rendered = key; removeAllViews()
        caption("Controller access", true)
        val pending = state.pending
        if (pending == null) caption(requestNotice ?: if (state.windowOpen)
            "Pairing is open. Find DX340 on your phone and confirm the system pairing prompts. A request appears here afterward."
            else "No new phone waiting. Saved controllers can reconnect from their Setup screen.")
        else {
            caption("Requesting access: ${pending.label}")
            if (!pending.bonded) caption("Complete Bluetooth pairing on both devices first.")
            else if (!state.windowOpen) caption("Open pairing again before approving this phone.")
            else if (state.approved.size >= 8 && state.approved.none { it.address == pending.address }) caption("Revoke an unused controller before adding another.")
            else caption("Check that this is your phone, then approve it here.")
            action("Approve ${pending.label}", pending.bonded && state.windowOpen && (state.approved.size < 8 || state.approved.any { it.address == pending.address })) {
                runningService()?.takeIf { it.epoch == state.hostEpoch }?.approve(pending.address)
            }
        }
        if (state.approved.isEmpty()) caption("No approved controllers yet.")
        else action("${if (showApproved) "Hide" else "Show"} approved controllers (${state.approved.size})") { showApproved = !showApproved }
        if (showApproved) {
            caption("Revoking access disconnects that controller; music keeps playing.")
            state.approved.forEach { controller ->
                caption("${controller.label}\n${controller.connection}")
                action("Revoke access for ${controller.label}") {
                    val running = runningService()
                    if (running != null) running.revokeController(controller.address) else store.revoke(controller.address)
                }
            }
        }
    }
}

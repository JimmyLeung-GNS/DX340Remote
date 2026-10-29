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

/** Local owner controls only; never exposed as a command on the BLE protocol. */
@SuppressLint("MissingPermission")
class ControllerManagementPanel(context: Context, private val changed: () -> Unit,
    private val store: ControllerApprovalStore = ControllerApprovalStore(context),
    private val runningService: () -> HostService? = { HostService.instance }) : LinearLayout(context) {
    private var rendered = ""
    init { orientation = VERTICAL }
    private fun caption(text: String, heading: Boolean = false) {
        addView(TextView(context).apply { this.text = text; textSize = if (heading) 22f else 16f; if (heading) setAccessibilityHeading(true) })
    }
    private fun action(text: String, enabled: Boolean = true, run: () -> Unit) {
        addView(Button(context).apply { this.text = text; isEnabled = enabled; minHeight = (48 * resources.displayMetrics.density).toInt(); setOnClickListener { run(); changed() } })
    }
    private fun label(address: String, device: BluetoothDevice?): String {
        val name = runCatching { device?.name }.getOrNull()?.replace(Regex("[\\p{Cntrl}\\u202A-\\u202E\\u2066-\\u2069]"), "")?.trim()?.take(60)?.takeIf { it.isNotEmpty() } ?: "Controller"
        return "$name · ${address.takeLast(8)}"
    }
    fun render(service: HostService?) {
        val ids = store.identities().sorted()
        val canReadBluetooth = context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val bonded = if (canReadBluetooth) runCatching {
            context.getSystemService(BluetoothManager::class.java).adapter?.takeIf { it.isEnabled }?.bondedDevices?.associateBy { it.address }
        }.getOrNull() else null
        val pending = service?.pending
        val pendingAddress = pending?.address
        val pendingLabel = pendingAddress?.let { label(it, if (canReadBluetooth) pending else null) }
        val pendingBonded = canReadBluetooth && runCatching { pending?.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
        val windowOpen = service != null && service.pairingRemainingMs > 0
        val labels = ids.associateWith { label(it, bonded?.get(it)) }
        val current = service?.activeControllerAddress
        val key = listOf(ids, labels, bonded?.keys, pendingAddress, pendingLabel, pendingBonded, windowOpen, current, service?.epoch).toString()
        if (key == rendered) return
        rendered = key; removeAllViews()
        caption("Controller access", true)
        caption("${ids.size} approved. Revoking access disconnects that controller; music keeps playing.")
        if (ids.isEmpty()) caption("No approved controllers yet.")
        ids.forEach { address ->
            // Disambiguate duplicate friendly names/suffixes with their full local identity.
            val display = labels.getValue(address).let { if (labels.values.count { value -> value == it } > 1) "$it · $address" else it }
            val state = when { current == address -> "Current controller"; bonded == null -> "Bluetooth status unavailable"; address in bonded -> "Paired"; else -> "Pair again to reconnect" }
            caption("$display\n$state")
            action("Revoke access for $display") {
                val running = runningService()
                if (running != null) running.revokeController(address) else store.revoke(address)
            }
        }
        if (pendingAddress == null) caption("No controller awaiting approval.")
        else {
            caption("Requesting access: $pendingLabel")
            if (!pendingBonded) caption("Complete Bluetooth pairing on both devices first.")
            else if (!windowOpen) caption("Open pairing again before approving this controller.")
            else if (ids.size >= 8 && pendingAddress !in ids) caption("Revoke an unused controller before adding another.")
            val expectedHost = service?.epoch
            action("Approve $pendingLabel", pendingBonded && windowOpen && (ids.size < 8 || pendingAddress in ids)) {
                runningService()?.takeIf { it.epoch == expectedHost }?.approve(pendingAddress)
            }
        }
    }
}

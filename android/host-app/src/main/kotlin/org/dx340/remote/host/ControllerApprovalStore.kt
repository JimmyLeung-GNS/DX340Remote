package org.dx340.remote.host

import android.bluetooth.BluetoothAdapter
import android.content.Context

/** Existing controllers.xml Boolean keys are preserved; OS bonds are never edited here. */
class ControllerApprovalStore(context: Context, preferencesName: String = "controllers") {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    fun identities(): Set<String> = preferences.all.filter { (key, value) -> value == true && BluetoothAdapter.checkBluetoothAddress(key) }.keys.toSet()
    fun contains(address: String) = address in identities()
    fun approve(address: String) {
        require(BluetoothAdapter.checkBluetoothAddress(address)) { "INVALID_CONTROLLER" }
        val current = identities()
        check(address in current || current.size < 8) { "CONTROLLER_LIMIT" }
        if (address !in current) preferences.edit().putBoolean(address, true).apply()
    }
    fun revoke(address: String): Boolean {
        require(BluetoothAdapter.checkBluetoothAddress(address)) { "INVALID_CONTROLLER" }
        if (!contains(address)) return false
        preferences.edit().remove(address).apply()
        return true
    }
    fun retainBonded(bonded: Set<String>?) {
        // Bluetooth off, missing permission or an unavailable adapter is not evidence
        // that every saved controller was unpaired.
        if (bonded == null) return
        val stale = identities() - bonded
        if (stale.isNotEmpty()) preferences.edit().apply { stale.forEach { remove(it) } }.apply()
    }
    fun revokeAll() {
        val ids = identities()
        if (ids.isNotEmpty()) preferences.edit().apply { ids.forEach { remove(it) } }.apply()
    }
}

package org.dx340.remote.host

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

internal class HostControlStore(context: Context) {
    private val prefs = context.getSharedPreferences("host-control", Context.MODE_PRIVATE)
    val enabled get() = prefs.getBoolean("enabled", false)
    fun setEnabled(enabled: Boolean): Boolean = prefs.edit().putBoolean("enabled", enabled).commit().also {
        if (!it) Log.e("DX340Remote", "Unable to persist phone-control choice")
    }
    fun choice() = HostControlChoice({ enabled }, ::setEnabled)

    companion object {
        fun permissionsGranted(context: Context) = listOf(Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE).all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

package org.dx340.remote.host

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** No boot receiver or scheduling: restore only the owner's saved choice after our own update. */
class HostPackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED ||
            !HostControlStore(context).enabled || !HostControlStore.permissionsGranted(context)) return
        try {
            context.startForegroundService(Intent(context, HostService::class.java).setAction("RESTORE"))
        } catch (e: RuntimeException) {
            // A vendor/OS restriction leaves local Enable available; never retry or open pairing.
            Log.w("DX340Remote", "Package-update restoration unavailable; use Enable phone control", e)
        }
    }
}

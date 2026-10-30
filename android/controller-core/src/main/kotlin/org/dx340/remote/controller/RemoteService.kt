package org.dx340.remote.controller

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.bluetooth.BluetoothAdapter
import android.os.Binder
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.Build

/** Owns the BLE session independently of Activity recreation; keeps no playback engine. */
class RemoteService : Service() {
    lateinit var model: RemoteModel; private set
    private val main = Handler(Looper.getMainLooper())
    private var reconciliationPending = false
    private var destroyed = false
    private val radioState = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                // Re-check the real adapter/permission state, not intent extras.
                model.bluetoothAvailabilityChanged()
            }
        }
    }
    inner class LocalBinder : Binder() { val service get() = this@RemoteService }
    override fun onCreate() {
        super.onCreate()
        model = RemoteModel(applicationContext)
        model.connectionObserver = ::reconcileForeground
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        // Bluetooth system broadcasts can originate from a privileged app UID.
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(radioState, filter, Context.RECEIVER_EXPORTED)
        else registerReceiver(radioState, filter)
    }
    private fun reconcileForeground() {
        if (destroyed || reconciliationPending) return
        reconciliationPending = true
        // Connect resets an old link before starting the new attempt; transport
        // loss similarly clears a link before choosing bounded retry or failure.
        // Judge the final state after that main-thread operation, not its reset.
        main.post {
            reconciliationPending = false
            if (!destroyed && !model.needsConnectionService) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }
    override fun onBind(intent: Intent): IBinder = LocalBinder()
    /** Explicit owner disconnect also removes foreground state while UI remains bound. */
    fun stopConnection() {
        model.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("remote", "DX340 connection", NotificationManager.IMPORTANCE_LOW))
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        val open = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        startForeground(341, Notification.Builder(this, "remote").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("DX340 Remote").setContentText("Bluetooth connection enabled").setContentIntent(open).setOngoing(true).build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        reconcileForeground()
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        destroyed = true
        main.removeCallbacksAndMessages(null)
        unregisterReceiver(radioState)
        model.connectionObserver = null
        model.close()
        super.onDestroy()
    }
}

package org.dx340.remote.controller

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/** Owns the BLE session independently of Activity recreation; keeps no playback engine. */
class RemoteService : Service() {
    lateinit var model: RemoteModel; private set
    inner class LocalBinder : Binder() { val service get() = this@RemoteService }
    override fun onCreate() { super.onCreate(); model = RemoteModel(applicationContext) }
    override fun onBind(intent: Intent): IBinder = LocalBinder()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("remote", "DX340 connection", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        startForeground(341, Notification.Builder(this, "remote").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("DX340 Remote").setContentText("Bluetooth connection enabled").setContentIntent(open).setOngoing(true).build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        return START_NOT_STICKY
    }
    override fun onDestroy() { model.close(); super.onDestroy() }
}

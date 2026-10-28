package org.dx340.remote.host
import android.service.notification.NotificationListenerService
class SessionListener : NotificationListenerService() {
    override fun onListenerConnected() { HostService.instance?.sources?.refresh() }
    override fun onListenerDisconnected() { HostService.instance?.sources?.refresh() }
}

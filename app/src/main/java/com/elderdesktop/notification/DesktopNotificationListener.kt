package com.elderdesktop.notification

import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.elderdesktop.DesktopSettings

class DesktopNotificationListener : NotificationListenerService() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        updateWakeLock()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val settings = DesktopSettings(this)
        if (!settings.enableDesktopNotifications) return

        sbn?.let {
            if (it.packageName == packageName) return@let // Ignore own notifications
            
            // Whitelist check
            if (!settings.notificationWhitelist.contains(it.packageName)) {
                Log.d("NotificationListener", "Ignoring non-whitelisted notification from ${it.packageName}")
                return@let
            }

            if (settings.enableFloatingNotifications) {
                FloatingNotificationManager.showNotification(this, it)
            } else {
                NotificationRepository.addNotification(it)
            }
            Log.d("NotificationListener", "Notification from ${it.packageName}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            NotificationRepository.removeNotification(it)
        }
    }

    private fun updateWakeLock() {
        val settings = DesktopSettings(this)
        if (settings.preventSleep) {
            if (wakeLock == null) {
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ElderDesktop::NotificationWakeLock")
                wakeLock?.acquire(10*60*1000L /*10 minutes*/)
            }
        } else {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        }
    }
}

package com.elderdesktop.notification

import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf

object NotificationRepository {
    val activeNotifications = mutableStateListOf<StatusBarNotification>()

    fun addNotification(sbn: StatusBarNotification) {
        val existingIndex = activeNotifications.indexOfFirst { it.key == sbn.key }
        if (existingIndex != -1) {
            activeNotifications[existingIndex] = sbn
        } else {
            activeNotifications.add(sbn)
        }
    }

    fun removeNotification(sbn: StatusBarNotification) {
        activeNotifications.removeAll { it.key == sbn.key }
    }
}

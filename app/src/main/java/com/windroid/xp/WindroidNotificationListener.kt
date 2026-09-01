package com.windroid.xp

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WindroidNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        publish(activeNotifications?.toList().orEmpty())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publish(activeNotifications?.toList().orEmpty())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publish(activeNotifications?.toList().orEmpty())
    }

    private fun publish(items: List<StatusBarNotification>) {
        val packages = items
            .filterNot { it.packageName == packageName }
            .filter { notification ->
                val flags = notification.notification.flags
                flags and Notification.FLAG_GROUP_SUMMARY == 0
            }
            .sortedByDescending { it.postTime }
            .map { it.packageName }
            .distinct()
        activePackages.value = packages
    }

    companion object {
        private val activePackages = MutableStateFlow<List<String>>(emptyList())
        val packages = activePackages.asStateFlow()
    }
}

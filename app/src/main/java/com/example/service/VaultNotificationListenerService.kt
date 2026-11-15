package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.VaultFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VaultNotificationListenerService : NotificationListenerService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return
    val packageName = sbn.packageName ?: return
    if (packageName == applicationContext.packageName) return

    val extras = sbn.notification?.extras ?: return
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

    val combined = listOf(title, bigText.ifBlank { text }).filter { it.isNotBlank() }.joinToString(" ")
    if (combined.isBlank()) return

    val app = applicationContext as? VaultFlowApplication ?: return
    serviceScope.launch {
      app.repository.processIncomingNotification(
        packageName = packageName,
        notificationText = combined,
        postTimeMillis = sbn.postTime,
      )
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }
}

package com.craysafe.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.craysafe.MainActivity
import com.craysafe.R
import com.craysafe.api.ApiClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CraySafeFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences("craysafe_prefs", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()

        val authToken = getSharedPreferences("craysafe_prefs", MODE_PRIVATE)
            .getString("auth_token", null)
        if (authToken != null) {
            serviceScope.launch {
                try {
                    ApiClient.apiService.saveFcmToken(
                        "Bearer $authToken",
                        token
                    )
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "CraySafe Alert"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Water quality alert"

        val alertId = message.data["alert_id"]?.toIntOrNull() ?: 0
        val priority = message.data["priority"] ?: "default"

        showNotification(title, body, alertId, priority)

        val intent = Intent("com.craysafe.REFRESH_BADGE")
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun showNotification(title: String, body: String, alertId: Int, priority: String) {

        val channelId = if (priority == "high") {
            NotificationHelper.CHANNEL_ID_CRITICAL
        } else {
            NotificationHelper.CHANNEL_ID_DEFAULT
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "alerts")
            putExtra("alert_id", alertId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            alertId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (priority == "high") NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(alertId, notification)
        } catch (e: SecurityException) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
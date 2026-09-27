package com.craysafe.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_ID_CRITICAL = "craysafe_critical"
    const val CHANNEL_ID_DEFAULT = "craysafe_alerts_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val criticalChannel = NotificationChannel(
                CHANNEL_ID_CRITICAL,
                "Critical Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate water quality dangers"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(criticalChannel)

            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "Warnings & Predictions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Warnings and Time-to-Danger predictions"
            }
            manager.createNotificationChannel(defaultChannel)
        }
    }
}
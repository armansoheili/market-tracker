package com.arman.markettracker.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.arman.markettracker.data.model.AlertRule
import com.arman.markettracker.data.model.Direction
import com.arman.markettracker.util.toPrice

object NotificationHelper {
    const val CHANNEL_ID = "price_alerts"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Price alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Notifies when a price reaches your target" }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun notifyAlert(context: Context, rule: AlertRule, assetName: String, price: Double) {
        val dir = if (rule.direction == Direction.ABOVE) "above" else "below"
        val text = "$assetName reached ${price.toPrice()} Toman ($dir ${rule.target.toPrice()})"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Price alert")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(rule.id.hashCode(), notification)
    }
}

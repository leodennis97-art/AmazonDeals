package com.example.amazondeals.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.amazondeals.MainActivity
import com.example.amazondeals.R
import com.example.amazondeals.data.model.Deal

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "amazon_deals_glitch_channel"
        const val CHANNEL_NAME = "Amazon Deal & Gutschein Alarme"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für 0.01€ Coupons & 50% Rabatt-Gutscheine"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendDealNotification(deal: Deal) {
        val targetUrl = deal.getFormattedUrl()
        val intent = if (targetUrl.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
        } else {
            Intent(context, MainActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            deal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (deal.isGlitch || deal.discountPercent >= 95) {
            "💥 PREISFEHLER (-${deal.discountPercent}%): ${deal.price}!"
        } else {
            "🏷️ NEUER GUTSCHEIN: ${deal.title}"
        }

        val origText = if (deal.originalPrice.isNotEmpty()) " (Statt ${deal.originalPrice})" else ""
        val body = "${deal.title}$origText\n${deal.couponText}\n${deal.botName}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(deal.id.hashCode(), builder.build())
    }
}

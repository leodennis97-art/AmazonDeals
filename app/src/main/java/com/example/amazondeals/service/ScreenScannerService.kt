package com.example.amazondeals.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.amazondeals.MainActivity
import com.example.amazondeals.data.repository.DealRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class ScreenScannerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())
    private var repository: DealRepository? = null

    companion object {
        private const val CHANNEL_ID = "screen_scanner_channel"
        private const val NOTIFICATION_ID = 888

        fun startService(context: Context) {
            try {
                val intent = Intent(context, ScreenScannerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, ScreenScannerService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = DealRepository(applicationContext)
        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        start247BackgroundScanning()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "24/7 KI Preis-Watcher",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Überprüft im Hintergrund automatisch, ob Produkte günstiger verfügbar sind."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚀 24/7 KI Preis-Watcher Aktiv")
            .setContentText("Scannt den Bildschirm & vergleicht Preise live im Netz...")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun start247BackgroundScanning() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                serviceScope.launch {
                    try {
                        val deals = repository?.fetchDeals() ?: emptyList()
                        val glitchDeal = deals.firstOrNull { it.isGlitch || it.discountPercent >= 90 }
                        if (glitchDeal != null) {
                            val resaleFormatted = String.format(Locale.GERMANY, "%.2f €", glitchDeal.getMyResalePriceEuros())
                            val marginFormatted = String.format(Locale.GERMANY, "+%.2f €", glitchDeal.getTradingProfitMarginEuros())
                            FloatingBubbleService.showBubble(
                                applicationContext,
                                glitchDeal.getFormattedUrl(),
                                glitchDeal.price,
                                resaleFormatted,
                                marginFormatted
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                handler.postDelayed(this, 30000)
            }
        }, 5000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

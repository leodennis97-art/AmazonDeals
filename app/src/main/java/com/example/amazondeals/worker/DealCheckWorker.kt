package com.example.amazondeals.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.amazondeals.data.repository.DealRepository
import com.example.amazondeals.notification.NotificationHelper
import java.util.concurrent.TimeUnit

class DealCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = DealRepository(context)
        val notificationHelper = NotificationHelper(context)
        val keywords = repository.getKeywords()

        try {
            val deals = repository.fetchDeals()
            for (deal in deals) {
                if (repository.isDealNotified(deal.id)) continue

                val fullText = "${deal.title} ${deal.description} ${deal.couponText} ${deal.price}".lowercase()
                val isMatch = keywords.any { kw -> fullText.contains(kw.lowercase()) } || deal.isGlitch

                if (isMatch) {
                    notificationHelper.sendDealNotification(deal)
                    repository.markDealAsNotified(deal.id)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_TAG = "amazon_deals_background_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DealCheckWorker>(15, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
        }
    }
}

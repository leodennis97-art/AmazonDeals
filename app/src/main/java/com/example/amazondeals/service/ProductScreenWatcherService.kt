package com.example.amazondeals.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.amazondeals.data.repository.DealRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale

class ProductScreenWatcherService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var repository: DealRepository? = null
    private var lastScannedText: String = ""
    private var lastScanTime: Long = 0L

    companion object {
        fun isAccessibilityEnabled(context: Context): Boolean {
            val prefString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            return prefString.contains(context.packageName)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = DealRepository(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        if (now - lastScanTime < 2000L) return // Debounce: max 1 check per 2 seconds
        lastScanTime = now

        val rootNode = rootInActiveWindow ?: return
        val detectedTextList = mutableListOf<String>()
        extractTextFromNode(rootNode, detectedTextList)

        // Find potential product title (longest meaningful string)
        val productTitle = detectedTextList
            .filter { it.length in 12..80 && !it.contains("http") && !it.contains("Anzeige") }
            .maxByOrNull { it.length }

        if (productTitle != null && productTitle != lastScannedText) {
            lastScannedText = productTitle
            searchAndShowCheapestBubble(productTitle)
        }
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            list.add(text)
        }
        for (i in 0 until node.childCount) {
            extractTextFromNode(node.getChild(i), list)
        }
    }

    private fun searchAndShowCheapestBubble(productTitle: String) {
        serviceScope.launch {
            try {
                val deals = repository?.fetchDeals() ?: emptyList()
                val matchedDeal = deals.firstOrNull { deal ->
                    productTitle.lowercase().split(" ").any { word ->
                        word.length > 3 && deal.title.lowercase().contains(word)
                    }
                }

                if (matchedDeal != null) {
                    val resaleFormatted = String.format(Locale.GERMANY, "%.2f €", matchedDeal.getMyResalePriceEuros())
                    val marginFormatted = String.format(Locale.GERMANY, "+%.2f €", matchedDeal.getTradingProfitMarginEuros())
                    FloatingBubbleService.showBubble(
                        applicationContext,
                        matchedDeal.getFormattedUrl(),
                        matchedDeal.price,
                        resaleFormatted,
                        marginFormatted
                    )
                } else {
                    val encodedTitle = URLEncoder.encode(productTitle, "UTF-8")
                    val searchUrl = "https://www.google.com/search?tbm=shop&q=$encodedTitle"
                    FloatingBubbleService.showBubble(
                        applicationContext,
                        searchUrl,
                        "Vergleich im Netz",
                        "Marktpreis",
                        "Aktiv"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInterrupt() {}
}

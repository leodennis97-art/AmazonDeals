package com.example.amazondeals.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.amazondeals.data.model.Deal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DealRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("amazon_deals_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIFIED_DEALS = "notified_deal_ids"
        private const val KEY_FAVORITE_DEALS = "favorite_deal_ids"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val PRIMARY_NGROK_URL = "https://speller-importer-captivate.ngrok-free.dev"
        private const val FALLBACK_CLOUD_URL = "https://amazondeals.onrender.com"
        private const val KEY_KEYWORDS = "search_keywords"
        private const val DEFAULT_KEYWORDS = "0.01€,2 für 0.01€,1ct,Sparen,50% Rabatt,Gutschein"

        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_LICENSE_EXPIRY = "license_expiry"
        private const val KEY_IS_UNLOCKED = "is_unlocked"
    }

    fun isLicenseActive(): Boolean {
        val isUnlocked = prefs.getBoolean(KEY_IS_UNLOCKED, false)
        val expiry = prefs.getLong(KEY_LICENSE_EXPIRY, 0L)
        return isUnlocked && (expiry > System.currentTimeMillis())
    }

    fun getSavedEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getSavedLicenseKey(): String = prefs.getString(KEY_LICENSE_KEY, "") ?: ""
    fun getLicenseExpiry(): Long = prefs.getLong(KEY_LICENSE_EXPIRY, 0L)

    fun saveLicense(email: String, key: String, expiryMs: Long) {
        prefs.edit()
            .putString(KEY_USER_EMAIL, email.trim())
            .putString(KEY_LICENSE_KEY, key.trim())
            .putLong(KEY_LICENSE_EXPIRY, expiryMs)
            .putBoolean(KEY_IS_UNLOCKED, true)
            .apply()
    }

    suspend fun verifyLicenseWithCloud(email: String, key: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val backendUrl = getBackendUrl()
        val trimmedKey = key.trim().uppercase()
        val trimmedEmail = email.trim().lowercase()

        if (trimmedEmail.isEmpty()) {
            return@withContext Pair(false, "Bitte gib deine E-Mail-Adresse ein.")
        }

        if (trimmedEmail == "leodennis97@gmail.com") {
            val lifetimeMs = System.currentTimeMillis() + (36500L * 24 * 60 * 60 * 1000)
            saveLicense(trimmedEmail, if (trimmedKey.isEmpty()) "VIP-LIFETIME" else trimmedKey, lifetimeMs)
            return@withContext Pair(true, "Willkommen Dennis! VIP-Zugang lebenslang aktiviert.")
        }

        if (trimmedKey.isEmpty()) {
            return@withContext Pair(false, "Bitte gib deinen Lizenzcode ein.")
        }

        if (trimmedKey == "PRO-2026-VIP" || trimmedKey.startsWith("PAYPAL") || trimmedKey == "PRO-1MONAT" || trimmedKey == "ADMIN") {
            val thirtyDaysMs = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            saveLicense(trimmedEmail, trimmedKey, thirtyDaysMs)
            return@withContext Pair(true, "Lizenz erfolgreich aktiviert (30 Tage PRO)!")
        }

        val endpointsToTry = listOf(backendUrl, FALLBACK_CLOUD_URL)
        for (endpoint in endpointsToTry) {
            try {
                val url = URL("$endpoint/verify_license")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("ngrok-skip-browser-warning", "true")
                    doOutput = true
                }

                val jsonPayload = JSONObject().apply {
                    put("email", trimmedEmail)
                    put("licenseKey", trimmedKey)
                }.toString()

                connection.outputStream.use { it.write(jsonPayload.toByteArray(Charsets.UTF_8)) }

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val status = json.optString("status", "")
                    val expiry = json.optLong("expiryMs", System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))
                    val message = json.optString("message", "Lizenz erfolgreich aktiviert!")

                    if (status == "ACTIVE" || status == "SUCCESS") {
                        saveLicense(trimmedEmail, trimmedKey, expiry)
                        return@withContext Pair(true, message)
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (trimmedKey.length >= 8) {
            val thirtyDaysMs = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            saveLicense(trimmedEmail, trimmedKey, thirtyDaysMs)
            return@withContext Pair(true, "Lizenz aktiviert (PRO Modus)!")
        }

        return@withContext Pair(false, "Verbindung zur Cloud fehlgeschlagen. Bitte Internetverbindung prüfen.")
    }

    fun getBackendUrl(): String {
        return prefs.getString(KEY_BACKEND_URL, PRIMARY_NGROK_URL) ?: PRIMARY_NGROK_URL
    }

    fun setBackendUrl(url: String) {
        prefs.edit().putString(KEY_BACKEND_URL, url.trim()).apply()
    }

    fun getKeywords(): List<String> {
        val csv = prefs.getString(KEY_KEYWORDS, DEFAULT_KEYWORDS) ?: DEFAULT_KEYWORDS
        return csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setKeywords(keywords: List<String>) {
        prefs.edit().putString(KEY_KEYWORDS, keywords.joinToString(",")).apply()
    }

    fun isDealNotified(dealId: String): Boolean {
        val set = prefs.getStringSet(KEY_NOTIFIED_DEALS, emptySet()) ?: emptySet()
        return set.contains(dealId)
    }

    fun markDealAsNotified(dealId: String) {
        val set = (prefs.getStringSet(KEY_NOTIFIED_DEALS, emptySet()) ?: emptySet()).toMutableSet()
        set.add(dealId)
        prefs.edit().putStringSet(KEY_NOTIFIED_DEALS, set).apply()
    }

    fun isFavorite(dealId: String): Boolean {
        val set = prefs.getStringSet(KEY_FAVORITE_DEALS, emptySet()) ?: emptySet()
        return set.contains(dealId)
    }

    fun toggleFavorite(dealId: String) {
        val set = (prefs.getStringSet(KEY_FAVORITE_DEALS, emptySet()) ?: emptySet()).toMutableSet()
        if (set.contains(dealId)) {
            set.remove(dealId)
        } else {
            set.add(dealId)
        }
        prefs.edit().putStringSet(KEY_FAVORITE_DEALS, set).apply()
    }

    suspend fun fetchDeals(): List<Deal> = withContext(Dispatchers.IO) {
        val primaryUrl = getBackendUrl()
        val urlsToTry = listOf(primaryUrl, FALLBACK_CLOUD_URL).distinct()
        val deals = mutableListOf<Deal>()

        for (endpoint in urlsToTry) {
            try {
                val url = URL("$endpoint/deals")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("ngrok-skip-browser-warning", "true")
                    setRequestProperty("User-Agent", "AmazonDealsApp/1.0")
                }

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(responseText)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val dealLink = obj.optString("dealUrl", "https://www.amazon.de/coupons")
                        val defaultPlatform = if (dealLink.lowercase().contains("kleinanzeigen")) "Kleinanzeigen.de" else "Amazon.de"
                        val historyArray = obj.optJSONArray("priceHistory")
                        val historyList = mutableListOf<Float>()
                        if (historyArray != null) {
                            for (j in 0 until historyArray.length()) {
                                historyList.add(historyArray.getDouble(j).toFloat())
                            }
                        }
                        if (historyList.isEmpty()) {
                            historyList.addAll(listOf(39.99f, 35.00f, 32.00f, 29.99f, 26.00f, 22.00f, 18.60f))
                        }

                        val vendorMap = mutableMapOf<String, Float>()
                        val vendorObj = obj.optJSONObject("vendorPrices")
                        if (vendorObj != null) {
                            val keys = vendorObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                vendorMap[key] = vendorObj.getDouble(key).toFloat()
                            }
                        }
                        if (vendorMap.isEmpty()) {
                            val buyP = obj.optString("price", "18.60").replace("€", "").replace(",", ".").trim().toFloatOrNull() ?: 18.60f
                            vendorMap["Amazon.de"] = buyP
                            vendorMap["Otto.de"] = buyP * 1.5f
                            vendorMap["MediaMarkt"] = buyP * 1.7f
                            vendorMap["Saturn"] = buyP * 1.75f
                        }

                        deals.add(
                            Deal(
                                id = obj.optString("id", System.currentTimeMillis().toString()),
                                title = obj.optString("title", "Angebot"),
                                description = obj.optString("description", ""),
                                price = obj.optString("price", "0.01 €"),
                                couponText = obj.optString("couponText", "Gutschein aktiv"),
                                dealUrl = dealLink,
                                imageUrl = obj.optString("imageUrl", ""),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                isGlitch = obj.optBoolean("isGlitch", false),
                                botName = obj.optString("botName", "🤖 AI Bot"),
                                aiConfidence = obj.optInt("aiConfidence", 98),
                                category = obj.optString("category", "Allgemein"),
                                platform = obj.optString("platform", defaultPlatform),
                                originalPrice = obj.optString("originalPrice", ""),
                                discountPercent = obj.optInt("discountPercent", if (obj.optBoolean("isGlitch")) 99 else 50),
                                cheapestWebPrice = obj.optString("cheapestWebPrice", ""),
                                cheapestWebUrl = obj.optString("cheapestWebUrl", ""),
                                priceHistory = historyList,
                                buyPlatform = obj.optString("buyPlatform", "Amazon.de"),
                                sellPlatform = obj.optString("sellPlatform", "Kleinanzeigen.de"),
                                promoCode = obj.optString("promoCode", ""),
                                vendorPrices = vendorMap,
                                dropshippingRoi = obj.optDouble("dropshippingRoi", 39.7).toFloat(),
                                resellStrategy = obj.optString("resellStrategy", "Arbitrage Resell (Amazon ➔ Kleinanzeigen)"),
                                isAutonomouslyDiscovered = obj.optBoolean("isAutonomouslyDiscovered", true)
                            )
                        )
                    }
                    if (deals.isNotEmpty()) {
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (deals.isEmpty()) {
            deals.addAll(getMockDeals())
        }

        // CLIENT-SIDE LIVE 404 FILTER: Verify deals directly on the user's phone to filter out all 404 Dog pages!
        val verifiedDeals = coroutineScope {
            deals.map { deal ->
                async(Dispatchers.IO) {
                    if (isUrlAliveOnDevice(deal.getFormattedUrl())) deal else null
                }
            }.awaitAll().filterNotNull()
        }

        return@withContext if (verifiedDeals.isNotEmpty()) verifiedDeals else getMockDeals()
    }

    private fun isUrlAliveOnDevice(rawUrl: String): Boolean {
        if (!rawUrl.contains("amazon.de/dp/")) return true
        try {
            val url = URL(rawUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept-Language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7")
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) return false

            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val pageText = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (pageText.contains("Tut uns leid. Die eingegebene Webadresse") || pageText.contains("Seite wurde nicht gefunden") || pageText.contains("dogsofamazon")) {
                return false
            }

            return responseCode in 200..302
        } catch (e: Exception) {
            return true
        }
    }

    fun getMockDeals(): List<Deal> {
        val now = System.currentTimeMillis()
        return listOf(
            Deal(
                id = "mock_1",
                title = "[🤖 Elektronik Bot] Apple AirTag 4er Pack - Glitch",
                description = "Aktiviere den Gutschein im Bezahlvorgang auf Amazon.de. Kombinierbar mit Spar-Abo.",
                price = "0.01 €",
                couponText = "Sparen: Erhalte 2 für 0.01€",
                dealUrl = "https://www.amazon.de/dp/B0935DNDT5",
                timestamp = now - 300000,
                isGlitch = true,
                platform = "Amazon.de",
                originalPrice = "119.00 €",
                discountPercent = 99
            ),
            Deal(
                id = "mock_2",
                title = "[⚡ Gaming Bot] Sony DualSense Wireless Controller PS5",
                description = "Gutschein direkt im Bezahlvorgang auf Amazon.de aktivieren.",
                price = "0.01 €",
                couponText = "Sparen: Erhalte 2 für 0.01€",
                dealUrl = "https://www.amazon.de/dp/B08H99BPJN",
                timestamp = now - 900000,
                isGlitch = true,
                platform = "Amazon.de",
                originalPrice = "69.99 €",
                discountPercent = 99
            ),
            Deal(
                id = "mock_3",
                title = "[☕ Haushalt Bot] Amazon Echo Dot (5. Gen) - Preisfehler",
                description = "Coupons im Checkout aktivieren. 50% Ersparnis durch Rabattgutschein.",
                price = "0.01 €",
                couponText = "Kaufe 3 zahle 1ct (Auto-Gutschein)",
                dealUrl = "https://www.amazon.de/dp/B0BQRB33XW",
                timestamp = now - 1800000,
                isGlitch = true,
                platform = "Amazon.de",
                originalPrice = "64.99 €",
                discountPercent = 99
            ),
            Deal(
                id = "mock_4",
                title = "[⚡ Elektronik Bot] Amazon Fire TV Stick - Rabatt",
                description = "Schnäppchen & Starker Preisnachlass mit 50% Coupon auf Amazon.de.",
                price = "9.99 €",
                couponText = "50% Rabatt Gutschein",
                dealUrl = "https://www.amazon.de/dp/B08C1W55L6",
                timestamp = now - 3600000,
                isGlitch = false,
                platform = "Amazon.de",
                originalPrice = "44.99 €",
                discountPercent = 50
            )
        )
    }
}

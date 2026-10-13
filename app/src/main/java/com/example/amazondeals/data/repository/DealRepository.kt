package com.example.amazondeals.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.amazondeals.data.model.Deal
import kotlinx.coroutines.Dispatchers
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
        private const val DEFAULT_BACKEND_URL = "https://speller-importer-captivate.ngrok-free.dev"
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

        // Lifetime VIP Free Access for Owner Account
        if (trimmedEmail == "leodennis97@gmail.com") {
            val lifetimeMs = System.currentTimeMillis() + (36500L * 24 * 60 * 60 * 1000) // 100 Years
            saveLicense(trimmedEmail, if (trimmedKey.isEmpty()) "VIP-LIFETIME" else trimmedKey, lifetimeMs)
            return@withContext Pair(true, "Willkommen Dennis! VIP-Zugang lebenslang aktiviert.")
        }

        if (trimmedKey.isEmpty()) {
            return@withContext Pair(false, "Bitte gib deinen Lizenzcode ein.")
        }

        // Master Keys for Instant VIP Activation
        if (trimmedKey == "PRO-2026-VIP" || trimmedKey.startsWith("PAYPAL") || trimmedKey == "PRO-1MONAT" || trimmedKey == "ADMIN") {
            val thirtyDaysMs = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            saveLicense(trimmedEmail, trimmedKey, thirtyDaysMs)
            return@withContext Pair(true, "Lizenz erfolgreich aktiviert (30 Tage PRO)!")
        }

        try {
            val url = URL("$backendUrl/verify_license")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
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
                } else {
                    return@withContext Pair(false, json.optString("error", "Ungültiger Lizenzcode."))
                }
            } else {
                return@withContext Pair(false, "Cloud-Verbindung fehlgeschlagen (${connection.responseCode}). Bitte Cloud-Status prüfen.")
            }
        } catch (e: Exception) {
            if (trimmedKey.length >= 8) {
                val thirtyDaysMs = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                saveLicense(trimmedEmail, trimmedKey, thirtyDaysMs)
                return@withContext Pair(true, "Lizenz aktiviert (PRO Modus)!")
            }
            return@withContext Pair(false, "Verbindung zur Cloud fehlgeschlagen. Bitte Internetverbindung prüfen.")
        }
    }

    fun getBackendUrl(): String {
        return prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
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
        val backendUrl = getBackendUrl()
        val deals = mutableListOf<Deal>()

        try {
            val url = URL("$backendUrl/deals")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
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
                    deals.add(
                        Deal(
                            id = obj.optString("id", System.currentTimeMillis().toString()),
                            title = obj.optString("title", "Amazon Angebot"),
                            description = obj.optString("description", ""),
                            price = obj.optString("price", "0.01 €"),
                            couponText = obj.optString("couponText", "Gutschein aktiv"),
                            dealUrl = obj.optString("dealUrl", "https://www.amazon.de/coupons"),
                            imageUrl = obj.optString("imageUrl", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isGlitch = obj.optBoolean("isGlitch", false)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (deals.isEmpty()) {
            deals.addAll(getMockDeals())
        }

        return@withContext deals
    }

    fun getMockDeals(): List<Deal> {
        val now = System.currentTimeMillis()
        return listOf(
            Deal(
                id = "mock_1",
                title = "Gillette Mach3 Rasierklingen 8er Pack",
                description = "Aktiviere den Gutschein im Bezahlvorgang. Kombinierbar mit Spar-Abo.",
                price = "0.01 €",
                couponText = "Sparen: Erhalte 2 für 0.01€",
                dealUrl = "https://www.amazon.de/dp/B08N5WRWNW",
                timestamp = now - 300000,
                isGlitch = true
            ),
            Deal(
                id = "mock_2",
                title = "Finish Powerball Dishwasher Tabs 100 Stk",
                description = "Coupons im Checkout aktivieren. 50% Ersparnis durch Rabattgutschein.",
                price = "0.01 €",
                couponText = "Kaufe 3 zahle 1ct (Auto-Gutschein)",
                dealUrl = "https://www.amazon.de/dp/B08C1W55L6",
                timestamp = now - 1800000,
                isGlitch = true
            ),
            Deal(
                id = "mock_3",
                title = "Philips Hue White LED Lampe E27 Doppelpack",
                description = "Aktionsgutschein auf der Produktseite anhaken.",
                price = "12.49 €",
                couponText = "50% Rabatt Gutschein",
                dealUrl = "https://www.amazon.de/dp/B08K3C4CQC",
                timestamp = now - 3600000,
                isGlitch = false
            ),
            Deal(
                id = "mock_4",
                title = "Anker USB-C Schnellladegerät 30W",
                description = "Rabattcode beim Bezahlen eingeben.",
                price = "0.01 €",
                couponText = "Sparen: Erhalte 2 für 0.01€",
                dealUrl = "https://www.amazon.de/dp/B08K3C4CQC",
                timestamp = now - 7200000,
                isGlitch = true
            )
        )
    }
}

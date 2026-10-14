package com.example.amazondeals.data.model

import android.net.Uri
import java.util.Locale

data class Deal(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val couponText: String,
    val dealUrl: String,
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isGlitch: Boolean = false,
    val botName: String = "🤖 AI Bot",
    val aiConfidence: Int = 98,
    val category: String = "Allgemein",
    val platform: String = "Amazon.de",
    val originalPrice: String = "",
    val discountPercent: Int = 99,
    val cheapestWebPrice: String = "",
    val cheapestWebUrl: String = ""
) {
    fun isMegaGlitch(): Boolean = discountPercent >= 95 || isGlitch

    fun getEstimatedResalePriceEuros(): Double {
        val purchase = price.replace("€", "").replace(",", ".").replace("(Gratis)", "").trim().toDoubleOrNull() ?: 1.0
        val orig = originalPrice.replace("€", "").replace(",", ".").trim().toDoubleOrNull() ?: (purchase * 2.0)
        val resale = if (orig > purchase) orig * 0.85 else purchase * 1.8
        return (resale).coerceAtLeast(purchase + 5.0)
    }

    fun getTradingProfitMarginEuros(): Double {
        val purchase = price.replace("€", "").replace(",", ".").replace("(Gratis)", "").trim().toDoubleOrNull() ?: 0.01
        return (getEstimatedResalePriceEuros() - purchase).coerceAtLeast(0.0)
    }

    fun getTradingProfitMarginPercent(): Int {
        val purchase = price.replace("€", "").replace(",", ".").replace("(Gratis)", "").trim().toDoubleOrNull() ?: 0.01
        if (purchase <= 0) return 100
        return ((getTradingProfitMarginEuros() / purchase) * 100).toInt()
    }

    fun getKleinanzeigenTemplateText(): String {
        val resalePriceFormatted = String.format(Locale.GERMANY, "%.2f €", getEstimatedResalePriceEuros())
        return """
            Neu & OVP: $title
            
            Preis: $resalePriceFormatted (Festpreis)
            Zustand: Fabrikneu & Ungeöffnet in Originalverpackung
            
            Hallo! Ich verkaufe hier ein nagelneues, unbenutztes $title.
            
            Details:
            - Zustand: 100% Neu, unbenutzt & in OVP
            - Aus tierfreiem Nichtraucherhaushalt
            - Abholung vor Ort oder schneller Versand möglich
            
            Bei Interesse oder Fragen einfach kurz eine Nachricht schreiben!
        """.trimIndent()
    }

    fun getCheapestComparisonUrl(): String {
        if (cheapestWebUrl.isNotBlank()) return cheapestWebUrl
        val cleanTitle = title.replace(Regex("""[^\w\s]"""), "").trim()
        return "https://www.google.com/search?tbm=shop&q=${Uri.encode(cleanTitle)}"
    }

    fun getClaimInstructions(): List<String> {
        val qty = getSuggestedQuantity()
        val textLower = "$title $description $couponText".lowercase()

        return when {
            qty > 1 || isGlitch || discountPercent >= 99 -> listOf(
                "1. Klicke unten auf 'Zum Amazon Produkt'.",
                "2. Lege exakt $qty Stück in deinen Warenkorb.",
                "3. Der Preisfehler (0,01 € / Gratis) wird an der Kasse automatisch abgezogen."
            )
            couponText.contains("50%") || textLower.contains("50%") || textLower.contains("gutschein") -> listOf(
                "1. Klicke unten auf 'Zum Amazon Produkt'.",
                "2. Aktiviere das Häkchen bei 'Gutschein einlösen' unter dem Preis.",
                "3. Der 50% Rabatt wird im letzten Bestellschritt abgezogen."
            )
            else -> listOf(
                "1. Klicke unten auf 'Zum Amazon Produkt'.",
                "2. Aktiviere etwaige Coupons auf der Produktseite.",
                "3. Schließe die Bestellung ab."
            )
        }
    }

    fun getFormattedUrl(): String {
        val trimmed = dealUrl.trim()

        val asin = getAsin()
        if (asin != null) {
            return "https://www.amazon.de/dp/$asin"
        }

        if (trimmed.isEmpty() || !trimmed.contains("amazon.de")) {
            return "https://www.amazon.de/coupons"
        }

        return ensureHttps(trimmed)
    }

    private fun ensureHttps(url: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("www.") -> "https://$url"
            else -> "https://www.$url"
        }
    }

    fun getAsin(): String? {
        val trimmed = dealUrl.trim()
        val regex = Regex("""(?i)(?:/dp/|/gp/product/|/product/|\bASIN=|\basin=|\bASIN\.1=)([B][A-Z0-9]{9})\b""")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.groupValues[1].uppercase()
        }
        val fallbackRegex = Regex("""\b(B[0-9A-Z]{9})\b""")
        return fallbackRegex.find(trimmed)?.groupValues?.get(1)?.uppercase()
    }

    fun getSuggestedQuantity(): Int {
        val combinedText = "$title $description $couponText".lowercase()
        return when {
            combinedText.contains("3 für") || combinedText.contains("kaufe 3") -> 3
            combinedText.contains("2 für") || combinedText.contains("erhalte 2") || combinedText.contains("2x") -> 2
            else -> 1
        }
    }

    fun getDirectCartUrl(): String {
        val asin = getAsin()
        return if (asin != null) {
            val qty = getSuggestedQuantity()
            "https://www.amazon.de/gp/item-dispatch?asin.1=$asin&action=addToCart&quantity.1=$qty"
        } else {
            getFormattedUrl()
        }
    }
}

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
    val cheapestWebUrl: String = "",
    val priceHistory: List<Float> = listOf(39.99f, 35.00f, 32.00f, 29.99f, 26.00f, 22.00f, 18.60f),
    val buyPlatform: String = "Amazon.de",
    val sellPlatform: String = "Kleinanzeigen.de"
) {
    fun isMegaGlitch(): Boolean = discountPercent >= 95 || isGlitch

    fun getLowestKleinanzeigenMarketPriceEuros(): Double {
        val purchase = price.replace("€", "").replace(",", ".").replace("(Gratis)", "").trim().toDoubleOrNull() ?: 1.0
        val orig = originalPrice.replace("€", "").replace(",", ".").trim().toDoubleOrNull() ?: (purchase * 2.0)
        val resale = if (orig > purchase) orig * 0.85 else purchase * 1.8
        return (resale).coerceAtLeast(purchase + 5.0)
    }

    fun getMyResalePriceEuros(): Double {
        // EXACTLY 1 Cent below the lowest current Kleinanzeigen market price!
        val lowestMarket = getLowestKleinanzeigenMarketPriceEuros()
        return (lowestMarket - 0.01).coerceAtLeast(0.01)
    }

    fun getTradingProfitMarginEuros(): Double {
        val purchase = price.replace("€", "").replace(",", ".").replace("(Gratis)", "").trim().toDoubleOrNull() ?: 0.01
        return (getMyResalePriceEuros() - purchase).coerceAtLeast(0.0)
    }

    fun getTradingProfitMarginPercent(): Int {
        val purchase = price.replace("€", "").replace(",", ".").replace("(Gratis)", "").trim().toDoubleOrNull() ?: 0.01
        if (purchase <= 0) return 100
        return ((getTradingProfitMarginEuros() / purchase) * 100).toInt()
    }

    fun getKleinanzeigenTemplateText(): String {
        val resalePriceFormatted = String.format(Locale.GERMANY, "%.2f €", getMyResalePriceEuros())
        return """
            Fabrikneu & OVP: $title
            
            Preis: $resalePriceFormatted (Günstigster Preis auf Kleinanzeigen - Festpreis)
            Zustand: Neu & Ungeöffnet in Originalverpackung
            
            Hallo! Ich verkaufe hier das fabriksneue, originale $title.
            
            Details:
            - Zustand: 100% Neu, unbenutzt & in OVP
            - Aus tierfreiem Nichtraucherhaushalt
            - Abholung vor Ort oder schneller Versand möglich
            
            Bei Interesse gerne direkt melden!
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
                "1. Öffne das Produkt unten auf Amazon.de (Seite zeigt vollen Preis an).",
                "2. Lege exakt $qty Stück in den Warenkorb.",
                "3. An der Kasse (im Checkout) wird der Preisfehler auf 0,01 € reduziert!"
            )
            couponText.contains("50%") || textLower.contains("50%") || textLower.contains("gutschein") -> listOf(
                "1. Öffne das Produkt unten auf Amazon.de (Seite zeigt vollen Preis an).",
                "2. WICHTIG: Setze das Häkchen bei 'Gutschein einlösen' direkt unter dem Preis!",
                "3. Der 50% Rabatt wird im letzten Bestellschritt an der Kasse abgezogen."
            )
            else -> listOf(
                "1. Öffne das Produkt unten auf Amazon.de.",
                "2. WICHTIG: Aktiviere den Coupon / Rabattgutschein auf der Produktseite.",
                "3. Der reduzierte Endpreis erscheint im Checkout."
            )
        }
    }

    fun getFormattedUrl(): String {
        val asin = getAsin()
        if (asin != null) {
            return "https://www.amazon.de/dp/$asin/?_encoding=UTF8&pd_rd_i=$asin&ref_=sbx_be_s_sparkle_ssd_tt&pd_rd_plhdr=t"
        }

        val trimmed = dealUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        return "https://www.amazon.de/dp/$trimmed/?_encoding=UTF8&pd_rd_i=$trimmed&ref_=sbx_be_s_sparkle_ssd_tt&pd_rd_plhdr=t"
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

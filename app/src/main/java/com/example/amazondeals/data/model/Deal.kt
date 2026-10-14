package com.example.amazondeals.data.model

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
    val discountPercent: Int = 99
) {
    fun isMegaGlitch(): Boolean = discountPercent >= 95 || isGlitch

    fun isKleinanzeigen(): Boolean {
        return platform.lowercase().contains("kleinanzeigen") || dealUrl.lowercase().contains("kleinanzeigen.de")
    }

    fun isEbay(): Boolean {
        return platform.lowercase().contains("ebay") || dealUrl.lowercase().contains("ebay.de")
    }

    fun getFormattedUrl(): String {
        val trimmed = dealUrl.trim()
        
        if (isKleinanzeigen()) {
            if (trimmed.isEmpty() || !trimmed.contains("kleinanzeigen.de")) {
                return "https://www.kleinanzeigen.de/s-zu-verschenken/k0"
            }
            return ensureHttps(trimmed)
        }

        if (isEbay()) {
            if (trimmed.isEmpty() || !trimmed.contains("ebay.de")) {
                return "https://www.ebay.de/b/Restposten/bn_7115312781"
            }
            return ensureHttps(trimmed)
        }

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
        if (isKleinanzeigen() || isEbay()) return null
        val trimmed = dealUrl.trim()
        val regex = Regex("""(?i)(?:/dp/|/gp/product/|/product/|\bASIN=|\basin=|\bASIN\.1=)([A-Z0-9]{10})\b""")
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
        if (isKleinanzeigen() || isEbay()) return getFormattedUrl()
        val asin = getAsin()
        return if (asin != null) {
            val qty = getSuggestedQuantity()
            "https://www.amazon.de/gp/item-dispatch?asin.1=$asin&action=addToCart&quantity.1=$qty"
        } else {
            getFormattedUrl()
        }
    }
}

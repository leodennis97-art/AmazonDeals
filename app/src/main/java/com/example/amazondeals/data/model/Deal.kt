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
    fun isKleinanzeigen(): Boolean {
        return platform.lowercase().contains("kleinanzeigen") || dealUrl.lowercase().contains("kleinanzeigen.de")
    }

    fun getFormattedUrl(): String {
        val trimmed = dealUrl.trim()
        if (isKleinanzeigen()) {
            if (trimmed.isEmpty() || !trimmed.contains("kleinanzeigen.de")) {
                return "https://www.kleinanzeigen.de/s-zu-verschenken/k0"
            }
            return when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                trimmed.startsWith("www.") -> "https://$trimmed"
                else -> "https://www.$trimmed"
            }
        }

        val asin = getAsin()
        if (asin != null) {
            return "https://www.amazon.de/dp/$asin"
        }

        if (trimmed.isEmpty() || !trimmed.contains("amazon.de")) {
            return "https://www.amazon.de/coupons"
        }

        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("www.") -> "https://$trimmed"
            trimmed.startsWith("amazon.de") -> "https://www.$trimmed"
            else -> "https://$trimmed"
        }
    }

    fun getAsin(): String? {
        if (isKleinanzeigen()) return null
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
        if (isKleinanzeigen()) return getFormattedUrl()
        val asin = getAsin()
        return if (asin != null) {
            val qty = getSuggestedQuantity()
            "https://www.amazon.de/gp/item-dispatch?asin.1=$asin&action=addToCart&quantity.1=$qty"
        } else {
            getFormattedUrl()
        }
    }
}

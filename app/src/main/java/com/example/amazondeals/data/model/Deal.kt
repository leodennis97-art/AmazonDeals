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
    val isGlitch: Boolean = false
) {
    fun getFormattedUrl(): String {
        val trimmed = dealUrl.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.isNotEmpty() -> "https://$trimmed"
            else -> "https://www.amazon.de/coupons"
        }
    }

    fun getAsin(): String? {
        val url = getFormattedUrl()
        val regex = Regex("""(?i)(?:/dp/|/gp/product/|/product/|\bASIN=|\basin=|\bASIN\.1=)([A-Z0-9]{10})\b""")
        val match = regex.find(url)
        if (match != null) {
            return match.groupValues[1].uppercase()
        }
        val fallbackRegex = Regex("""\b(B[0-9A-Z]{9})\b""")
        return fallbackRegex.find(url)?.groupValues?.get(1)?.uppercase()
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

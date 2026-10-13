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
)

package com.example.amazondeals.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HighEndDarkColorScheme = darkColorScheme(
    primary = AmazonAmberPrimary,
    onPrimary = Color.Black,
    primaryContainer = AmazonAmberContainer,
    onPrimaryContainer = OnAmazonAmberContainer,
    secondary = CouponEmerald,
    onSecondary = Color.Black,
    secondaryContainer = CouponEmeraldContainer,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = GlitchCrimson,
    onTertiary = Color.White,
    tertiaryContainer = GlitchCrimsonContainer,
    onTertiaryContainer = Color(0xFFFECACA),
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF475569)
)

private val HighEndLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9900),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFF7ED),
    onPrimaryContainer = Color(0xFF9A3412),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFFDC2626),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF2F2),
    onTertiaryContainer = Color(0xFF991B1B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun AmazonDealsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false for consistent branded Amazon High-End look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HighEndDarkColorScheme else HighEndLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

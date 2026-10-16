package com.example.amazondeals.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Toast
import com.example.amazondeals.service.ProductScreenWatcherService
import com.example.amazondeals.service.ScreenScannerService
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amazondeals.data.model.Deal
import java.util.Locale

fun openUrlSafely(context: Context, rawUrl: String, isCartUrl: Boolean = false) {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return

    val formattedUrl = when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("www.") -> "https://$trimmed"
        trimmed.startsWith("amazon.de") -> "https://www.$trimmed"
        else -> "https://$trimmed"
    }

    try {
        val uri = Uri.parse(formattedUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // DIRECT LAUNCH: Open directly in the native Amazon App if installed!
        try {
            intent.setPackage("com.amazon.mShop.android.shopping")
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Amazon App not installed, open in default Web Browser / Chrome
            intent.setPackage(null)
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DealViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showVipManagementDialog by remember { mutableStateOf(false) }
    var selectedTradingDeal by remember { mutableStateOf<Deal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF00E676), Color(0xFFFFD700))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💸", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CashFlow AI PRO",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Automatisierte Online-Einkommens Platform",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                },
                actions = {
                    if (state.isLicenseActive) {
                        Surface(
                            color = Color(0xFFFF9900).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF9900)),
                            modifier = Modifier.clickable { showVipManagementDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("👑 VIP", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9900))
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(onClick = { viewModel.refreshDeals() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Header Banner
            StatusHeaderCard(
                lastUpdated = state.lastUpdated,
                dealsCount = state.filteredDeals.size,
                totalSavings = state.totalSavingsEuros,
                backendUrl = state.backendUrl,
                context = context
            )

            // Filter Chips
            FilterChipsRow(
                selectedFilter = state.selectedFilter,
                favoritesCount = state.favorites.size,
                onSelectFilter = { viewModel.applyFilter(it) }
            )

            // Deals List (Top Ersparnisse an der Spitze)
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Scanne Amazon.de nach Glitches...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (state.filteredDeals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Keine passenden Angebote gefunden.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Versuche einen anderen Filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.filteredDeals, key = { _, deal -> deal.id }) { index, deal ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(250)) + slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = spring(stiffness = Spring.StiffnessLow)
                            )
                        ) {
                            DealCard(
                                deal = deal,
                                rankIndex = if (index < 3) index else -1,
                                selectedFilterName = state.selectedFilter,
                                isFavorite = viewModel.isFavorite(deal.id),
                                onToggleFavorite = { viewModel.toggleFavorite(deal.id) },
                                onOpenTradingDialog = { selectedTradingDeal = it },
                                context = context
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentUrl = state.backendUrl,
            onDismiss = { showSettingsDialog = false },
            onSave = { newUrl ->
                viewModel.updateBackendUrl(newUrl)
                showSettingsDialog = false
            }
        )
    }

    if (showVipManagementDialog) {
        VipManagementDialog(
            state = state,
            onDismiss = { showVipManagementDialog = false }
        )
    }

    selectedTradingDeal?.let { deal ->
        VipTradingDialog(
            deal = deal,
            context = context,
            onDismiss = { selectedTradingDeal = null }
        )
    }
}

@Composable
fun StatusHeaderCard(
    lastUpdated: Long,
    dealsCount: Int,
    totalSavings: Double,
    backendUrl: String,
    context: Context
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "📈 $dealsCount Aktive Einkommens-Quellen (CashFlow AI Engine)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF00E676)
                )
            }

            if (totalSavings > 0) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "💰 ${String.format(Locale.GERMANY, "%.2f €", totalSavings)}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: String,
    favoritesCount: Int,
    onSelectFilter: (String) -> Unit
) {
    val filters = listOf(
        "Alle" to "⚡ Alle Einkommen",
        "Produkt-Flipping" to "🛒 Produkt-Flipping",
        "Website-Flipping" to "🌐 Website-Flipping",
        "High-Affiliate" to "💰 High-Commission Affiliate",
        "Favoriten" to "❤️ Favoriten ($favoritesCount)"
    )

    LazyRow(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(filters) { (key, label) ->
            FilterChip(
                selected = selectedFilter == key,
                onClick = { onSelectFilter(key) },
                label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun DealCard(
    deal: Deal,
    rankIndex: Int = -1,
    selectedFilterName: String = "Alle",
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenTradingDialog: (Deal) -> Unit,
    context: Context
) {
    var expanded by remember { mutableStateOf(false) }
    val categoryTag = if (selectedFilterName == "Alle") "AN DER SPITZE" else "IN $selectedFilterName".uppercase()
    val productUrl = deal.getFormattedUrl()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = if (rankIndex >= 0) 1.5.dp else 1.dp,
            color = if (rankIndex >= 0) Color(0xFFFF9900) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Dynamic Category Rank Badge at top of card
            val marginFormatted = String.format(Locale.GERMANY, "%.2f €", deal.getTradingProfitMarginEuros())
            if (rankIndex == 0) {
                Surface(
                    color = Color(0xFFFF9900).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF9900)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "🏆 #1 HÖCHSTE MARGE $categoryTag: +$marginFormatted",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9900)
                    )
                }
            } else if (rankIndex == 1) {
                Surface(
                    color = Color(0xFFFF9900).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF9900).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "🥈 #2 HÖCHSTE MARGE $categoryTag: +$marginFormatted",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9900)
                    )
                }
            } else if (rankIndex == 2) {
                Surface(
                    color = Color(0xFFFF9900).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF9900).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "🥉 #3 HÖCHSTE MARGE $categoryTag: +$marginFormatted",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9900)
                    )
                }
            }

            // Header Row: Badge & Price & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (deal.isGlitch) {
                        Surface(
                            color = Color(0xFFEF4444),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "⚡ ECHTER PREISFEHLER VERIFIZIERT",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "🛒 AMAZON.DE GUTSCHEIN",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "-${deal.discountPercent}%",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (deal.originalPrice.isNotEmpty()) {
                        Text(
                            text = deal.originalPrice,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = TextStyle(textDecoration = TextDecoration.LineThrough)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = deal.price,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorit",
                            tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title (Tapping title directly opens the Amazon product page!)
            Text(
                text = deal.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                modifier = Modifier.clickable {
                    openUrlSafely(context, productUrl, isCartUrl = false)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Coupon Badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "🏷️ ${deal.couponText}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (deal.promoCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏷️ RABATTCODE: ${deal.promoCode}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Rabattcode", deal.promoCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Rabattcode '${deal.promoCode}' kopiert! 📋", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Code kopieren", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Description (Expandable)
            if (deal.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                AnimatedVisibility(visible = expanded || deal.description.length < 80) {
                    Text(
                        text = deal.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (deal.description.length >= 80) {
                    Text(
                        text = if (expanded) "Weniger anzeigen" else "Details anzeigen...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step-by-Step Claim Instructions Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "💡 So sicherst du dir den Rabatt:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    deal.getClaimInstructions().forEach { step ->
                        Text(
                            text = step,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Amazon Coupon Activation Warning Banner
            Surface(
                color = Color(0xFFFF9900).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFF9900)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ HINWEIS: Amazon zeigt vorerst den regulären Preis an. Auf Amazon das Kästchen 'Gutschein einlösen' ankreuzen!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9900),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Wo Kaufen & Wo Verkaufen Arbitrage Dashboard
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFF9900).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🛒 KAUFPREIS (NEU & OVP):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${deal.buyPlatform} (${deal.price})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9900))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🏷️ KLEINANZEIGEN TIEFSTPREIS:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.GERMANY, "%.2f €", deal.getLowestKleinanzeigenMarketPriceEuros()), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⚡ DEIN VERKAUFSPREIS (1ct BILLIGER):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.GERMANY, "%.2f €", deal.getMyResalePriceEuros()), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFFF9900).copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📈 DROPSHIPPING / ARBITRAGE ROI:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Text("+${String.format(Locale.GERMANY, "%.1f %%", deal.dropshippingRoi)} ROI", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Price Trend Chart Diagram (Shows all searched web stores & lowest node)
            PriceTrendChart(
                priceHistory = deal.priceHistory,
                vendorPrices = deal.vendorPrices,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // KI Web Price Comparison Container
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🤖 KI-Günstigster Netzpreis:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA)
                        )
                        Text(
                            text = if (deal.cheapestWebPrice.isNotEmpty()) deal.cheapestWebPrice else "Günstigstes Produkt im Netz",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            openUrlSafely(context, deal.getCheapestComparisonUrl(), isCartUrl = false)
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF60A5FA)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Im Netz prüfen",
                            fontSize = 10.sp,
                            color = Color(0xFF60A5FA),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            val productUrl = deal.getFormattedUrl()

            Button(
                onClick = {
                    openUrlSafely(context, productUrl, isCartUrl = false)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "👉 Zum Amazon Produkt / Angebot",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // VIP Trading & Share Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenTradingDialog(deal) },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                ) {
                    Text(
                        text = "📈 VIP Resell (+${String.format(Locale.GERMANY, "%.2f €", deal.getTradingProfitMarginEuros())})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🔥 Amazon Glitch Deal (${deal.discountPercent}% Ersparnis): ${deal.title} für ${deal.price}!\n$productUrl"
                            )
                        }
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(Intent.createChooser(shareIntent, "Deal teilen"))
                    },
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Teilen", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun VipTradingDialog(
    deal: Deal,
    context: Context,
    onDismiss: () -> Unit
) {
    val templateText = deal.getKleinanzeigenTemplateText()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📈 VIP Trading & Marge", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = deal.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2
                )

                // Margin Calculation Card
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🛒 Kaufpreis (Neu & OVP):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(deal.price, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🏷️ Niedrigster Kleinanzeigen Marktpreis:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.GERMANY, "%.2f €", deal.getLowestKleinanzeigenMarketPriceEuros()), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⚡ Dein Verkaufspreis (1 Cent günstiger):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text(String.format(Locale.GERMANY, "%.2f €", deal.getMyResalePriceEuros()), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFF10B981).copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💰 Ihre Reingewinn-Marge:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text(
                                text = "+${String.format(Locale.GERMANY, "%.2f €", deal.getTradingProfitMarginEuros())} (+${deal.getTradingProfitMarginPercent()}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Text("📋 Kleinanzeigen Verkaufsvorlage:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                OutlinedTextField(
                    value = templateText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    textStyle = TextStyle(fontSize = 11.sp),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Kleinanzeigen Vorlage", templateText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Vorlage in Zwischenablage kopiert! 📋", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vorlage für Kleinanzeigen kopieren", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

data class AdminManagedUser(
    val email: String,
    var role: String,
    var isBanned: Boolean = false,
    var assignedKey: String = ""
)

@Composable
fun VipManagementDialog(
    state: DealUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var generatedKey by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }

    val usersList = remember {
        mutableStateListOf(
            AdminManagedUser("leodennis97@gmail.com", "Master VIP Owner"),
            AdminManagedUser("kunde_paypal@gmx.de", "PRO (PayPal)", assignedKey = "PRO-2026-PAYPAL"),
            AdminManagedUser("user_test2026@web.de", "Standard (Gratis)"),
            AdminManagedUser("kauf_interessent@gmail.com", "Standard (Gratis)")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👑 VIP Admin Panel & Kundenverwaltung", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Tab Selector
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFFF9900)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Mein Status", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Kunden-Verwaltung", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (activeTab == 0) {
                    // Current User Status
                    Surface(
                        color = Color(0xFFFF9900).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF9900))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("VIP Status:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Aktiv & Verifiziert 👑", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9900))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Angemeldete E-Mail:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (state.savedEmail.isNotEmpty()) state.savedEmail else "leodennis97@gmail.com", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Aktiver Schlüssel:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (state.savedLicenseKey.isNotEmpty()) state.savedLicenseKey else "VIP-LIFETIME", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("💻 Gerät-Verwaltung:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Dieses Android-Gerät ist registriert und freigeschaltet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("• Unbegrenzte Push-Benachrichtigungen aktiv.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    // Admin Customer Management Panel
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("👥 Alle Kunden & Benutzer ({${usersList.size}}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        usersList.forEachIndexed { index, user ->
                            Surface(
                                color = if (user.isBanned) Color(0xFFEF4444).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (user.isBanned) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = user.email,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (user.isBanned) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (user.isBanned) "🚫 GEBANNT" else user.role,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (user.isBanned) Color(0xFFEF4444) else Color(0xFFFF9900)
                                        )
                                    }

                                    if (user.assignedKey.isNotEmpty()) {
                                        Text(
                                            text = "Schlüssel: ${user.assignedKey}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF10B981)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Action Buttons Row: Ban / Lizenz / Admin
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Ban Button
                                        OutlinedButton(
                                            onClick = {
                                                usersList[index] = user.copy(isBanned = !user.isBanned)
                                                val statusMsg = if (!user.isBanned) "Benutzer gebannt!" else "Benutzer entbannt!"
                                                Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, if (user.isBanned) Color(0xFF10B981) else Color(0xFFEF4444))
                                        ) {
                                            Text(
                                                text = if (user.isBanned) "🔓 Entbannen" else "🚫 Bannen",
                                                fontSize = 9.sp,
                                                color = if (user.isBanned) Color(0xFF10B981) else Color(0xFFEF4444),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Give License Button
                                        OutlinedButton(
                                            onClick = {
                                                val generated = "PRO-VIP-" + (100000..999999).random()
                                                usersList[index] = user.copy(role = "PRO (VIP)", assignedKey = generated, isBanned = false)
                                                Toast.makeText(context, "VIP Lizenz erteilt: $generated", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF10B981))
                                        ) {
                                            Text(
                                                text = "🔑 Lizenz",
                                                fontSize = 9.sp,
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Make Admin Button
                                        OutlinedButton(
                                            onClick = {
                                                usersList[index] = user.copy(role = "Admin / Owner", isBanned = false)
                                                Toast.makeText(context, "${user.email} ist jetzt Admin!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFFF9900))
                                        ) {
                                            Text(
                                                text = "👑 Admin",
                                                fontSize = 9.sp,
                                                color = Color(0xFFFF9900),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("🔑 Lizenz Generator:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        Button(
                            onClick = {
                                val newKey = "PRO-VIP-" + (100000..999999).random()
                                generatedKey = newKey
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
                        ) {
                            Text("Neuen 30-Tage Lizenzschlüssel erzeugen", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        if (generatedKey.isNotEmpty()) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(generatedKey, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF10B981))
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Lizenzcode", generatedKey)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Lizenzcode kopiert!", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Kopieren", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Fertig")
            }
        }
    )
}

@Composable
fun PriceTrendChart(
    priceHistory: List<Float>,
    vendorPrices: Map<String, Float> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (priceHistory.size < 2) return

    val minPrice = priceHistory.minOrNull() ?: 10f
    val maxPrice = priceHistory.maxOrNull() ?: 50f
    val range = (maxPrice - minPrice).coerceAtLeast(1f)
    val lowestVendorPair = vendorPrices.minByOrNull { it.value }?.toPair() ?: Pair("Amazon.de", minPrice)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("📊 Durchsuchte Internet-Shops & Preistrend:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("🟢 Tiefster Punkt: ${lowestVendorPair.first} (${String.format(Locale.GERMANY, "%.2f €", lowestVendorPair.second.toDouble())})", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Vendor Price Badges Row
        if (vendorPrices.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                items(vendorPrices.toList()) { (store, p) ->
                    val isLowest = store == lowestVendorPair.first
                    Surface(
                        color = if (isLowest) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isLowest) Color(0xFF10B981) else Color(0xFF334155))
                    ) {
                        Text(
                            text = "$store: ${String.format(Locale.GERMANY, "%.2f €", p.toDouble())}",
                            fontSize = 9.sp,
                            fontWeight = if (isLowest) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLowest) Color(0xFF10B981) else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            val width = size.width
            val height = size.height
            val points = mutableListOf<Offset>()

            priceHistory.forEachIndexed { index, price ->
                val x = (width / (priceHistory.size - 1)) * index
                val normalizedY = (price - minPrice) / range
                val y = height - (normalizedY * height)
                points.add(Offset(x, y))
            }

            for (i in 0 until points.size - 1) {
                drawLine(
                    color = Color(0xFF10B981),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3f
                )
            }

            val lastPoint = points.last()
            drawCircle(
                color = Color(0xFFEF4444),
                radius = 6f,
                center = lastPoint
            )
        }
    }
}

@Composable
fun SettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cloud Backend Verbindung", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Cloud / ngrok Backend URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

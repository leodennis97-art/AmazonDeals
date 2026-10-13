package com.example.amazondeals.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amazondeals.data.model.Deal

fun openUrlSafely(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val trimmed = url.trim()
        val formattedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
        val uri = Uri.parse(formattedUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())), "Öffnen mit")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DealViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF9900), Color(0xFFFF5500))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛒", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AmazonDeals PRO",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "24/7 Glitch & Coupon Scanner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
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
                backendUrl = state.backendUrl,
                onTestNotification = { viewModel.sendTestNotification() },
                onRequestPermission = onRequestNotificationPermission
            )

            // Search Bar
            SearchBarSection(
                query = state.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // Filter Chips
            FilterChipsRow(
                selectedFilter = state.selectedFilter,
                favoritesCount = state.favorites.size,
                onSelectFilter = { viewModel.applyFilter(it) }
            )

            // Deals List
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
                            "Versuche einen anderen Filter oder Suchbegriff.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.filteredDeals, key = { it.id }) { deal ->
                        DealCard(
                            deal = deal,
                            isFavorite = viewModel.isFavorite(deal.id),
                            onToggleFavorite = { viewModel.toggleFavorite(deal.id) },
                            context = context
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentUrl = state.backendUrl,
            currentKeywords = state.keywords.joinToString(", "),
            onDismiss = { showSettingsDialog = false },
            onSave = { newUrl, newKeywords ->
                viewModel.updateBackendUrl(newUrl)
                viewModel.updateKeywords(
                    newKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun StatusHeaderCard(
    lastUpdated: Long,
    dealsCount: Int,
    backendUrl: String,
    onTestNotification: () -> Unit,
    onRequestPermission: () -> Unit
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "24/7 Scanner Aktiv",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "$dealsCount Deals live",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (lastUpdated > 0) {
                    "Zuletzt gescannt: ${
                        DateUtils.getRelativeTimeSpanString(
                            lastUpdated,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        )
                    }"
                } else "Warte auf ersten Scan...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTestNotification,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test-Alarm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                OutlinedButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Rechte prüfen", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text("Angebote durchsuchen (z. B. Gillette, Tabs, 50%)...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Löschen")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun FilterChipsRow(
    selectedFilter: String,
    favoritesCount: Int,
    onSelectFilter: (String) -> Unit
) {
    val filters = listOf(
        "Alle" to "⚡ Alle Bots",
        "Drogerie Bot" to "🤖 Drogerie Bot",
        "Elektronik Bot" to "⚡ Elektronik Bot",
        "Haushalt Bot" to "☕ Haushalt Bot",
        "0.01€ / 1ct" to "💥 0.01€ / 1ct",
        "Glitch Deals" to "🔥 Glitches",
        "Favoriten" to "❤️ Favoriten ($favoritesCount)"
    )

    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (key, label) ->
            FilterChip(
                selected = selectedFilter == key,
                onClick = { onSelectFilter(key) },
                label = { Text(label, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun DealCard(
    deal: Deal,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    context: Context
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                                "⚡ PREISFEHLER / 1CT",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "GUTSCHEIN",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = deal.price,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorit",
                            tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = deal.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Coupon Badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "🏷️ ${deal.couponText}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Description (Expandable)
            if (deal.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(visible = expanded || deal.description.length < 80) {
                    Text(
                        text = deal.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (deal.description.length >= 80) {
                    Text(
                        text = if (expanded) "Weniger anzeigen" else "Details anzeigen...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            val directCartUrl = deal.getDirectCartUrl()
            val productUrl = deal.getFormattedUrl()
            val qty = deal.getSuggestedQuantity()

            Button(
                onClick = {
                    openUrlSafely(context, directCartUrl)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
            ) {
                Text(
                    text = "🛒 Direkt in Warenkorb legen (${qty}x)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        openUrlSafely(context, productUrl)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Produktseite", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🔥 Amazon Glitch Deal: ${deal.title} für ${deal.price}!\n$directCartUrl"
                            )
                        }
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(Intent.createChooser(shareIntent, "Deal teilen"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Teilen", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentUrl: String,
    currentKeywords: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    var keywords by remember { mutableStateOf(currentKeywords) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cloud Backend & Alarm-Filter", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Cloud Backend URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Alarm-Schlüsselwörter (kommagetrennt)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url, keywords) },
                shape = RoundedCornerShape(8.dp)
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

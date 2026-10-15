package com.example.amazondeals.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val PAYPAL_LINK = "https://www.paypal.com/ncp/payment/FK8JDAFFUGYMA"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(
    savedEmail: String,
    savedKey: String,
    isVerifying: Boolean,
    errorMessage: String?,
    onVerifyLicense: (String, String) -> Unit,
    onRegisterUser: (String, String) -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(savedEmail) }
    var password by remember { mutableStateOf("") }
    var licenseKey by remember { mutableStateOf(savedKey) }
    var isRegisterMode by remember { mutableStateOf(false) }

    val isOwner = email.trim().lowercase() == "leodennis97@gmail.com"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF9900), Color(0xFFFF5500))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Sperrbildschirm",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AmazonDeals PRO",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color.White
                )

                Text(
                    text = "24/7 Glitch & Coupon Scanner",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9900),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Paypal Payment Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_LINK))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC439))
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💳 1 Monat PRO Lizenz kaufen (PayPal)",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Anmelden / Registrieren
                TabRow(
                    selectedTabIndex = if (isRegisterMode) 1 else 0,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFFF9900)
                ) {
                    Tab(
                        selected = !isRegisterMode,
                        onClick = { isRegisterMode = false },
                        text = { Text("Anmelden", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = isRegisterMode,
                        onClick = { isRegisterMode = true },
                        text = { Text("Registrieren (Cloud)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // E-Mail Textfield
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-Mail Adresse") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isRegisterMode) {
                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Passwort festlegen") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    // License Key Input
                    OutlinedTextField(
                        value = if (isOwner && licenseKey.isEmpty()) "VIP-LIFETIME-OWNER" else licenseKey,
                        onValueChange = { licenseKey = it },
                        label = { Text(if (isOwner) "Lizenzcode (Nicht erforderlich)" else "Lizenzcode") },
                        placeholder = { Text("Erhältst du nach PayPal Zahlung", color = Color(0xFF64748B), fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        singleLine = true,
                        enabled = !isOwner,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (isOwner) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "👑 VIP Owner Account",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage != null && !isOwner) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button (Silent Registration/Login - Zero Toasts!)
                Button(
                    onClick = {
                        if (isRegisterMode) {
                            onRegisterUser(email, password)
                        } else {
                            onVerifyLicense(email, if (isOwner && licenseKey.isEmpty()) "VIP-LIFETIME-OWNER" else licenseKey)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isVerifying,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRegisterMode) "Konto in Cloud anlegen & Starten" else (if (isOwner) "👑 Als VIP starten" else "Anmelden & Starten"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

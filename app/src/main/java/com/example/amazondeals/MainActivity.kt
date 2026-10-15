package com.example.amazondeals

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.example.amazondeals.ui.HomeScreen
import com.example.amazondeals.ui.LockScreen
import com.example.amazondeals.ui.DealViewModel
import com.example.amazondeals.ui.theme.AmazonDealsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DealViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Benachrichtigungen aktiviert!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Benachrichtigungsrecht abgelehnt. Alarme stumm.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestNotificationPermission()

        setContent {
            AmazonDealsTheme {
                val state by viewModel.uiState.collectAsState()

                if (!state.isLicenseActive) {
                    LockScreen(
                        savedEmail = state.savedEmail,
                        savedKey = state.savedLicenseKey,
                        isVerifying = state.isVerifyingLicense,
                        errorMessage = state.licenseErrorMessage,
                        onVerifyLicense = { email, key ->
                            viewModel.verifyLicense(email, key)
                        },
                        onRegisterUser = { email, pass ->
                            viewModel.registerUser(email, pass)
                        }
                    )
                } else {
                    HomeScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

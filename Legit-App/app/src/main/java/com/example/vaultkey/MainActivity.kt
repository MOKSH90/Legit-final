package com.example.vaultkey

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.vaultkey.data.AppController
import com.example.vaultkey.data.LegitApi
import com.example.vaultkey.data.SessionStore
import com.example.vaultkey.navigations.AppNavHost
import com.example.vaultkey.ui.theme.VaultkeyTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : FragmentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "Notification permission granted")
        } else {
            Log.d("FCM", "Notification permission denied")
        }
    }
    private fun askNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

            when {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    Log.d("FCM", "Already granted")
                }

                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askNotificationPermission()
        val fcmTokenState = androidx.compose.runtime.mutableStateOf<String?>(null)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM", "Token: $token")
            fcmTokenState.value = token
        }

        setContent {
            VaultkeyTheme {
                val fcmToken by fcmTokenState
                val windowSizeClass = calculateWindowSizeClass(this)
                val navController = rememberNavController()
                val appController = remember {
                    AppController(
                        LegitApi(
                            sessionStore = SessionStore(applicationContext)
                        )
                    )
                }
                
                // Observe session and token changes to register token
                androidx.compose.runtime.LaunchedEffect(appController.session, fcmToken) {
                    val token = fcmToken
                    if (appController.session != null && token != null) {
                        appController.registerPushToken(token)
                    }
                }

                AppNavHost(
                    navController = navController,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    appController = appController
                )
            }
        }
    }
}

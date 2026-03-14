package org.xplore.project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import org.koin.android.ext.android.inject
import org.xplore.project.data.local.AppPreferences
import org.xplore.project.ui.util.LocalNotificationPermissionRequester
import org.xplore.project.ui.util.PendingDeepLink

class MainActivity : ComponentActivity() {

    private val appPreferences: AppPreferences by inject()

    private val notificationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            appPreferences.notificationsEnabled = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        syncNotificationPermissionState()
        handleNotificationIntent(intent)

        setContent {
            CompositionLocalProvider(
                LocalNotificationPermissionRequester provides ::requestNotificationPermission,
            ) {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val type = intent?.getStringExtra("type") ?: return
        if (type == "friend_request") {
            PendingDeepLink.send("open_friend_requests")
        }
    }

    private fun syncNotificationPermissionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (alreadyGranted) {
                appPreferences.notificationsEnabled = true
            } else if (!appPreferences.notificationsAsked) {
                appPreferences.notificationsAsked = true
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            appPreferences.notificationsEnabled = true
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                appPreferences.notificationsEnabled = true
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

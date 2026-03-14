package org.xplore.project

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.xplore.project.data.local.TokenManager
import org.xplore.project.domain.repository.NotificationRepository

/**
 * Handles FCM messages and token refresh.
 *
 * - [onNewToken]: invoked by FCM when the registration token is created or refreshed.
 *   Registers the new token with the backend (only when the user is logged in).
 * - [onMessageReceived]: invoked when a message arrives while the app is in the foreground.
 *   Builds and posts a system tray notification so the user sees it regardless of app state.
 */
class XploreFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val notificationRepository: NotificationRepository by inject()
    private val tokenManager: TokenManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val isLoggedIn = tokenManager.accessToken != null && !tokenManager.isGuest
        if (!isLoggedIn) return

        serviceScope.launch {
            runCatching {
                notificationRepository.registerDeviceToken(token, "android")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: return
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: return

        val type = remoteMessage.data["type"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", type)
            putExtra("friendshipId", remoteMessage.data["friendshipId"])
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, "xplore_default")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

package org.xplore.project.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.xplore.project.MainActivity

/**
 * Foreground service that keeps a persistent notification visible
 * while the user is navigating an itinerary.
 *
 * The notification shows: turn-by-turn instruction, next POI name,
 * distance, and stop progress.
 * It is updated silently (no sound/vibration) on each location tick.
 */
class NavigationService : Service() {

    companion object {
        const val CHANNEL_ID = "xplore_navigation"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START = "org.xplore.project.ACTION_START_NAV"
        const val ACTION_UPDATE = "org.xplore.project.ACTION_UPDATE_NAV"
        const val ACTION_STOP = "org.xplore.project.ACTION_STOP_NAV"

        const val EXTRA_POI_NAME = "poi_name"
        const val EXTRA_DISTANCE = "distance_meters"
        const val EXTRA_STOP_INDEX = "stop_index"
        const val EXTRA_TOTAL_STOPS = "total_stops"
        const val EXTRA_INSTRUCTION = "instruction"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNavigationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START, ACTION_UPDATE -> {
                val poiName = intent.getStringExtra(EXTRA_POI_NAME) ?: ""
                val distance = intent.getIntExtra(EXTRA_DISTANCE, 0)
                val stopIndex = intent.getIntExtra(EXTRA_STOP_INDEX, 1)
                val totalStops = intent.getIntExtra(EXTRA_TOTAL_STOPS, 1)
                val instruction = intent.getStringExtra(EXTRA_INSTRUCTION)

                val notification = buildNotification(
                    poiName, distance, stopIndex, totalStops, instruction,
                )

                if (intent.action == ACTION_START) {
                    startForeground(NOTIFICATION_ID, notification)
                } else {
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    // ── Notification building ────────────────────────────────

    private fun buildNotification(
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String?,
    ): Notification {
        val distText = if (distanceMeters < 1000) {
            "$distanceMeters m"
        } else {
            val km = distanceMeters / 1000.0
            "${(kotlin.math.round(km * 10) / 10.0)} km"
        }

        val title = "Prossima tappa: $poiName"
        val summaryLine = "$distText · Tappa $stopIndex di $totalStops"

        // Tap on notification opens the app
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingTap)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)

        if (instruction != null) {
            // Collapsed: show instruction as the content text
            // Expanded: show instruction + distance summary via BigTextStyle
            builder.setContentText(instruction)
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$instruction\n$summaryLine")
                    .setSummaryText(summaryLine)
            )
        } else {
            builder.setContentText(summaryLine)
        }

        return builder.build()
    }

    private fun createNavigationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Navigazione",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Notifica persistente durante la navigazione"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}

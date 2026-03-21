package org.xplore.project.platform

import org.xplore.project.domain.notification.NavigationNotifier
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNNotificationSound

/**
 * iOS implementation of [NavigationNotifier].
 *
 * Uses [UNUserNotificationCenter] to post / replace a local notification
 * with a fixed identifier so it updates in-place rather than spamming
 * the notification center.
 *
 * The notification is silent (no sound) and refreshed on each location tick.
 */
class IosNavigationNotifier : NavigationNotifier {

    private val notificationId = "xplore_navigation"

    override fun startNavigation(
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String?,
    ) {
        postNotification(poiName, distanceMeters, stopIndex, totalStops, instruction)
    }

    override fun updateNavigation(
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String?,
    ) {
        postNotification(poiName, distanceMeters, stopIndex, totalStops, instruction)
    }

    override fun stopNavigation() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removeDeliveredNotificationsWithIdentifiers(listOf(notificationId))
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(notificationId))
    }

    private fun postNotification(
        poiName: String,
        distanceMeters: Int,
        stopIndex: Int,
        totalStops: Int,
        instruction: String?,
    ) {
        val distText = if (distanceMeters < 1000) {
            "$distanceMeters m"
        } else {
            val km = distanceMeters / 1000.0
            "${(kotlin.math.round(km * 10) / 10.0)} km"
        }

        val summaryLine = "$distText · Tappa $stopIndex di $totalStops"

        val content = UNMutableNotificationContent().apply {
            setTitle("Prossima tappa: $poiName")
            if (instruction != null) {
                setBody("$instruction\n$summaryLine")
            } else {
                setBody(summaryLine)
            }
            // No sound to avoid spamming the user on every update
            setSound(null)
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notificationId,
            content = content,
            trigger = null, // Deliver immediately
        )

        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }
}

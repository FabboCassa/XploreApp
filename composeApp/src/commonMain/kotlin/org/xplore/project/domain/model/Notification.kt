package org.xplore.project.domain.model

/**
 * Types of push notifications the app can receive.
 */
enum class NotificationType {
    FRIEND_REQUEST,
    ACHIEVEMENT_REACHED,
    GROUP_INVITE,
    COMPETITION_ENDED,
}

/**
 * Domain model for a notification.
 */
data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val entityId: String?,
    val createdAt: String,
    val isRead: Boolean = false,
)

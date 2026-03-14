package org.xplore.project.domain.notification

import org.xplore.project.domain.model.NotificationType

/**
 * Represents a push notification received while the app is in the foreground.
 */
data class NotificationEvent(
    val type: NotificationType,
    val title: String,
    val body: String,
    val entityId: String?,
)

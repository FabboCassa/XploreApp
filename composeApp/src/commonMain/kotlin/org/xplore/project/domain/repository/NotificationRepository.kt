package org.xplore.project.domain.repository

/**
 * Repository interface for notification operations.
 */
interface NotificationRepository {
    suspend fun registerDeviceToken(token: String, platform: String)
}

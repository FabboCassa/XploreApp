package org.xplore.project.data.repository

import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.remote.NotificationApiService
import org.xplore.project.domain.repository.NotificationRepository

/**
 * Concrete implementation of [NotificationRepository].
 */
class NotificationRepositoryImpl(
    private val apiService: NotificationApiService,
    private val tokenManager: TokenManager,
) : NotificationRepository {

    override suspend fun registerDeviceToken(token: String, platform: String) {
        val authToken = tokenManager.accessToken
            ?: throw IllegalStateException("Not authenticated")
        apiService.registerDeviceToken(token, platform, authToken)
    }
}

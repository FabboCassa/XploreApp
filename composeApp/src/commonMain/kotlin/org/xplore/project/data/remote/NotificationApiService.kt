package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.xplore.project.data.remote.dto.RegisterDeviceTokenRequest

/**
 * Remote data source for the Notifications API endpoints.
 */
class NotificationApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    /** Register a device token for push notifications. */
    suspend fun registerDeviceToken(token: String, platform: String, authToken: String) {
        val response = httpClient.post("$baseUrl/api/notifications/device-token") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(RegisterDeviceTokenRequest(token, platform))
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }
}

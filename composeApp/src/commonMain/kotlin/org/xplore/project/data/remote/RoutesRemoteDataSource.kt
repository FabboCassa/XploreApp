package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.xplore.project.data.remote.dto.CreateSavedRouteRequest
import org.xplore.project.data.remote.dto.SavedRouteDto

/**
 * Remote data source for the Routes API endpoints.
 */
class RoutesRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    /** Get the current user's saved (not completed) routes. */
    suspend fun getSavedRoutes(): List<SavedRouteDto> {
        val response = httpClient.get("$baseUrl/api/routes/saved")
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Get the current user's completed routes. */
    suspend fun getCompletedRoutes(): List<SavedRouteDto> {
        val response = httpClient.get("$baseUrl/api/routes/completed")
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Create a new saved route. */
    suspend fun createRoute(request: CreateSavedRouteRequest): SavedRouteDto {
        val response = httpClient.post("$baseUrl/api/routes") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Mark a route as completed. */
    suspend fun markRouteCompleted(routeId: String) {
        val response = httpClient.post("$baseUrl/api/routes/$routeId/complete")
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Delete a saved route. */
    suspend fun deleteRoute(routeId: String) {
        val response = httpClient.delete("$baseUrl/api/routes/$routeId")
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Get a shared route by its share token (no auth needed). */
    suspend fun getSharedRoute(shareToken: String): SavedRouteDto {
        val response = httpClient.get("$baseUrl/api/routes/shared/$shareToken")
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }
}

package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.client.request.parameter
import org.xplore.project.data.remote.dto.MapPinDto
import io.ktor.http.isSuccess

/**
 * Remote data source that calls the backend's stateless POI proxy endpoint.
 *
 * The backend itself queries OpenStreetMap's Overpass API in real-time
 * and returns the results — it does NOT store POIs permanently.
 */
class MapPinRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    /**
     * Fetches POIs from the backend proxy for a given location and radius.
     *
     * @param lat Latitude of the center point.
     * @param lon Longitude of the center point.
     * @param radiusKm Search radius in kilometers.
     * @return List of [MapPinDto] received from the backend.
     */
    suspend fun fetchPins(lat: Double, lon: Double, radiusKm: Double): List<MapPinDto> {
        val response = httpClient.get("$baseUrl/api/map/pois") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("radius", radiusKm)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /**
     * Searches POIs by name via the backend search endpoint.
     */
    suspend fun searchPins(query: String, lat: Double, lon: Double, radiusKm: Double): List<MapPinDto> {
        val response = httpClient.get("$baseUrl/api/map/search") {
            parameter("query", query)
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("radius", radiusKm)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /**
     * Submits a rating for a specific POI.
     * The token is passed explicitly since the global HttpClient lacks the Auth plugin.
     */
    suspend fun ratePoi(poiId: String, score: Int, token: String) {
        val response = httpClient.post("$baseUrl/api/map/pois/$poiId/rate") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(mapOf("score" to score))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to submit rating: HTTP Error ${response.status.value}")
        }
    }
}

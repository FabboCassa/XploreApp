package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.xplore.project.data.remote.dto.MapPinDto

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
        return httpClient.get("$baseUrl/api/map/pois") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("radius", radiusKm)
        }.body()
    }
}

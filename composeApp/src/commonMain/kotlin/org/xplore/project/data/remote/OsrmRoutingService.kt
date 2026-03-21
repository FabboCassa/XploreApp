package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.xplore.project.domain.model.MapPin

/**
 * Route information returned by the OSRM service.
 *
 * @param geometryJson   GeoJSON FeatureCollection with the route LineString.
 * @param nextLegDistanceMeters Distance in meters to the next waypoint (first leg).
 * @param nextLegDurationSeconds Duration in seconds to the next waypoint (first leg).
 */
data class RouteInfo(
    val geometryJson: String,
    val nextLegDistanceMeters: Double? = null,
    val nextLegDurationSeconds: Double? = null,
)

/**
 * Calls the free OSRM public API to get road-following routes
 * between itinerary stops.
 *
 * API: https://router.project-osrm.org/route/v1/{profile}/{coords}
 * Returns a GeoJSON LineString geometry that follows actual roads.
 */
class OsrmRoutingService(
    private val httpClient: HttpClient,
) {

    /**
     * Fetches a road-following route geometry for the given stops.
     *
     * @param stops Ordered list of map pins to route through.
     * @param profile Routing profile: "foot", "car", or "bike".
     * @return [RouteInfo] with geometry and first-leg distance/duration,
     *         or null if the request fails.
     */
    suspend fun fetchRouteGeometry(
        stops: List<MapPin>,
        profile: String = "foot",
    ): RouteInfo? {
        if (stops.size < 2) return null

        val coords = stops.joinToString(";") { "${it.longitude},${it.latitude}" }
        val url = "https://router.project-osrm.org/route/v1/$profile/$coords?overview=full&geometries=geojson&steps=false"

        return try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) return null

            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val code = root["code"]?.jsonPrimitive?.content
            if (code != "Ok") return null

            val routes = root["routes"]?.jsonArray ?: return null
            val firstRoute = routes[0].jsonObject
            val geometry = firstRoute["geometry"] ?: return null
            val coordinates = geometry.jsonObject["coordinates"]?.jsonArray ?: return null

            // Extract first leg distance/duration
            val legs = firstRoute["legs"]?.jsonArray
            val firstLeg = legs?.firstOrNull()?.jsonObject
            val legDistance = firstLeg?.get("distance")?.jsonPrimitive?.doubleOrNull
            val legDuration = firstLeg?.get("duration")?.jsonPrimitive?.doubleOrNull

            // Wrap the LineString in a FeatureCollection for MapLibre
            val coordsStr = coordinates.joinToString(",") { coord ->
                val arr = coord.jsonArray
                "[${arr[0].jsonPrimitive.double},${arr[1].jsonPrimitive.double}]"
            }

            val geoJson = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coordsStr]},"properties":{}}]}"""

            RouteInfo(
                geometryJson = geoJson,
                nextLegDistanceMeters = legDistance,
                nextLegDurationSeconds = legDuration,
            )
        } catch (_: Exception) {
            null
        }
    }
}

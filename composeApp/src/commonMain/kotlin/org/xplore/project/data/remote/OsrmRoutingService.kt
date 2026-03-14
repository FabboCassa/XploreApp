package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.xplore.project.domain.model.MapPin

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
     * @return GeoJSON FeatureCollection string with the route LineString,
     *         or null if the request fails.
     */
    suspend fun fetchRouteGeometry(
        stops: List<MapPin>,
        profile: String = "foot",
    ): String? {
        if (stops.size < 2) return null

        val coords = stops.joinToString(";") { "${it.longitude},${it.latitude}" }
        val url = "https://router.project-osrm.org/route/v1/$profile/$coords?overview=full&geometries=geojson"

        return try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) return null

            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val code = root["code"]?.jsonPrimitive?.content
            if (code != "Ok") return null

            val routes = root["routes"]?.jsonArray ?: return null
            val geometry = routes[0].jsonObject["geometry"] ?: return null
            val coordinates = geometry.jsonObject["coordinates"]?.jsonArray ?: return null

            // Wrap the LineString in a FeatureCollection for MapLibre
            val coordsStr = coordinates.joinToString(",") { coord ->
                val arr = coord.jsonArray
                "[${arr[0].jsonPrimitive.double},${arr[1].jsonPrimitive.double}]"
            }

            """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coordsStr]},"properties":{}}]}"""
        } catch (_: Exception) {
            null
        }
    }
}

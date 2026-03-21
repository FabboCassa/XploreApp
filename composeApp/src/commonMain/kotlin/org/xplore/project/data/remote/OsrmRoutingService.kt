package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
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
 * @param nextInstruction Human-readable turn-by-turn instruction for the immediate next maneuver.
 */
data class RouteInfo(
    val geometryJson: String,
    val nextLegDistanceMeters: Double? = null,
    val nextLegDurationSeconds: Double? = null,
    val nextInstruction: String? = null,
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
        val url = "https://router.project-osrm.org/route/v1/$profile/$coords?overview=full&geometries=geojson&steps=true"

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

            // Extract turn-by-turn instruction from first leg steps
            val instruction = buildNextInstruction(firstLeg?.get("steps")?.jsonArray)

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
                nextInstruction = instruction,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds a human-readable navigation instruction from the OSRM steps array.
     *
     * Format: "Prosegui per [strada] per [distanza] poi [svolta] su [prossima strada]"
     *
     * OSRM step format:
     * ```json
     * { "maneuver": { "type": "turn", "modifier": "right" },
     *   "name": "Via Roma", "distance": 120.5 }
     * ```
     */
    private fun buildNextInstruction(steps: JsonArray?): String? {
        if (steps == null || steps.isEmpty()) return null

        try {
            val firstStep = steps[0].jsonObject
            val currentStreet = firstStep["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val stepDistance = firstStep["distance"]?.jsonPrimitive?.doubleOrNull ?: 0.0

            val distText = if (stepDistance < 1000) {
                "${stepDistance.toInt()} m"
            } else {
                val km = stepDistance / 1000.0
                "${(kotlin.math.round(km * 10) / 10.0)} km"
            }

            // If there's a second step, describe the upcoming turn
            val secondStep = steps.getOrNull(1)?.jsonObject
            val turnPart = if (secondStep != null) {
                val maneuver = secondStep["maneuver"]?.jsonObject
                val type = maneuver?.get("type")?.jsonPrimitive?.content ?: ""
                val modifier = maneuver?.get("modifier")?.jsonPrimitive?.content ?: ""
                val nextStreet = secondStep["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

                val turnText = maneuverToItalian(type, modifier)
                if (nextStreet != null) {
                    " poi $turnText su $nextStreet"
                } else if (turnText.isNotBlank()) {
                    " poi $turnText"
                } else null
            } else null

            // Build the full instruction
            return if (currentStreet != null) {
                "Prosegui per $currentStreet per $distText${turnPart ?: ""}"
            } else {
                "Prosegui per $distText${turnPart ?: ""}"
            }
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Translates OSRM maneuver type+modifier to Italian text.
     */
    private fun maneuverToItalian(type: String, modifier: String): String {
        return when (type) {
            "turn", "end of road", "new name" -> when (modifier) {
                "left" -> "gira a sinistra"
                "right" -> "gira a destra"
                "slight left" -> "tieni la sinistra"
                "slight right" -> "tieni la destra"
                "sharp left" -> "svolta secca a sinistra"
                "sharp right" -> "svolta secca a destra"
                "uturn" -> "fai inversione"
                "straight" -> "prosegui dritto"
                else -> "gira"
            }
            "roundabout", "rotary" -> when {
                modifier.contains("left") -> "alla rotonda esci a sinistra"
                modifier.contains("right") -> "alla rotonda esci a destra"
                else -> "alla rotonda esci"
            }
            "fork" -> when (modifier) {
                "left" -> "al bivio tieni la sinistra"
                "right" -> "al bivio tieni la destra"
                else -> "al bivio prosegui"
            }
            "merge" -> "immettiti"
            "depart" -> "parti"
            "arrive" -> "sei arrivato"
            else -> when (modifier) {
                "left" -> "gira a sinistra"
                "right" -> "gira a destra"
                "straight" -> "prosegui dritto"
                else -> ""
            }
        }
    }
}

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
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * Remote data source that calls the backend's stateless POI proxy endpoint.
 *
 * The backend itself queries OpenStreetMap's Overpass API in real-time
 * and returns the results — it does NOT store POIs permanently.
 *
 * ## Offline Fallback
 * If the backend is unreachable, this class falls back to querying
 * the public Overpass API (`https://overpass-api.de`) directly from
 * the app, mapping the raw OSM JSON to [MapPinDto].
 */
class MapPinRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    private val overpassUrl = "https://overpass-api.de/api/interpreter"
    private val overpassJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Fetches POIs from the backend proxy for a given location and radius.
     * Falls back to the public Overpass API if the backend is unreachable.
     */
    suspend fun fetchPins(lat: Double, lon: Double, radiusKm: Double): List<MapPinDto> {
        return try {
            val response = httpClient.get("$baseUrl/api/map/pois") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("radius", radiusKm)
            }
            if (!response.status.isSuccess()) {
                throw Exception("HTTP Error ${response.status.value}")
            }
            response.body()
        } catch (e: Exception) {
            println("🔌 [MapPins] Backend failed: ${e.message} — falling back to Overpass API")
            fetchFromOverpass(lat, lon, radiusKm)
        }
    }

    /**
     * Searches POIs by name via the backend search endpoint.
     * Falls back to the public Overpass API name search if the backend is unreachable.
     */
    suspend fun searchPins(query: String, lat: Double, lon: Double, radiusKm: Double): List<MapPinDto> {
        return try {
            val response = httpClient.get("$baseUrl/api/map/search") {
                parameter("query", query)
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("radius", radiusKm)
            }
            if (!response.status.isSuccess()) {
                throw Exception("HTTP Error ${response.status.value}")
            }
            response.body()
        } catch (e: Exception) {
            println("🔌 [MapPins] Backend search failed: ${e.message} — falling back to Overpass API")
            searchFromOverpass(query, lat, lon, radiusKm)
        }
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

    // ── Overpass Fallback ──────────────────────────────────────────

    /**
     * Queries the public Overpass API for POIs within a radius.
     * Mirrors the same OSM tags that the Xplore backend queries.
     */
    private suspend fun fetchFromOverpass(lat: Double, lon: Double, radiusKm: Double): List<MapPinDto> {
        val radiusM = (radiusKm * 1000).toInt()
        val query = buildOverpassQuery(lat, lon, radiusM)
        return executeOverpassQuery(query)
    }

    /**
     * Searches POIs by name within a radius via Overpass regex.
     */
    private suspend fun searchFromOverpass(name: String, lat: Double, lon: Double, radiusKm: Double): List<MapPinDto> {
        val radiusM = (radiusKm * 1000).toInt()
        val escapedName = name.replace("\"", "\\\"")
        val query = """
            [out:json][timeout:25];
            (
              node["name"~"$escapedName",i](around:$radiusM,$lat,$lon);
              way["name"~"$escapedName",i](around:$radiusM,$lat,$lon);
            );
            out center body;
        """.trimIndent()
        return executeOverpassQuery(query)
    }

    /**
     * Builds an OverpassQL query matching the same POI categories
     * that the Xplore backend (MapService) fetches.
     */
    private fun buildOverpassQuery(lat: Double, lon: Double, radiusM: Int): String {
        return """
            [out:json][timeout:25];
            (
              node["tourism"~"museum|gallery|artwork|attraction|viewpoint|information"](around:$radiusM,$lat,$lon);
              way["tourism"~"museum|gallery|artwork|attraction|viewpoint|information"](around:$radiusM,$lat,$lon);
              node["historic"](around:$radiusM,$lat,$lon);
              way["historic"](around:$radiusM,$lat,$lon);
              node["amenity"~"place_of_worship|theatre|arts_centre|community_centre"](around:$radiusM,$lat,$lon);
              way["amenity"~"place_of_worship|theatre|arts_centre|community_centre"](around:$radiusM,$lat,$lon);
              node["natural"~"peak|cliff|cave_entrance|water|spring"](around:$radiusM,$lat,$lon);
              way["natural"~"peak|cliff|cave_entrance|water|spring"](around:$radiusM,$lat,$lon);
              node["leisure"~"park|garden|nature_reserve"](around:$radiusM,$lat,$lon);
              way["leisure"~"park|garden|nature_reserve"](around:$radiusM,$lat,$lon);
            );
            out center body;
        """.trimIndent()
    }

    /**
     * Sends the OverpassQL query via HTTP GET and parses the JSON response
     * into a list of [MapPinDto].
     */
    private suspend fun executeOverpassQuery(query: String): List<MapPinDto> {
        return try {
            val response = httpClient.get(overpassUrl) {
                parameter("data", query)
            }
            if (!response.status.isSuccess()) {
                println("🔌 [Overpass] HTTP Error ${response.status.value}")
                return emptyList()
            }
            val rawJson = response.bodyAsText()
            parseOverpassResponse(rawJson)
        } catch (e: Exception) {
            println("🔌 [Overpass] Request failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Parses Overpass JSON response into [MapPinDto] list.
     * Handles both `node` (lat/lon) and `way` (center.lat/center.lon) elements.
     */
    private fun parseOverpassResponse(rawJson: String): List<MapPinDto> {
        return try {
            val root = overpassJson.parseToJsonElement(rawJson).jsonObject
            val elements = root["elements"]?.jsonArray ?: return emptyList()

            elements.mapNotNull { elem ->
                val obj = elem.jsonObject
                val tags = obj["tags"]?.jsonObject ?: return@mapNotNull null

                // Resolve coordinates: nodes have lat/lon, ways have center.lat/center.lon
                val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["center"]?.jsonObject?.get("lat")?.jsonPrimitive?.doubleOrNull
                    ?: return@mapNotNull null
                val lon = obj["lon"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["center"]?.jsonObject?.get("lon")?.jsonPrimitive?.doubleOrNull
                    ?: return@mapNotNull null

                val name = tags["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val osmId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val osmType = obj["type"]?.jsonPrimitive?.contentOrNull ?: "node"

                MapPinDto(
                    id = "osm_${osmType}_$osmId",
                    label = name,
                    latitude = lat,
                    longitude = lon,
                    type = classifyOsmTags(tags),
                    description = tags["description"]?.jsonPrimitive?.contentOrNull,
                    category = tags["tourism"]?.jsonPrimitive?.contentOrNull
                        ?: tags["historic"]?.jsonPrimitive?.contentOrNull
                        ?: tags["amenity"]?.jsonPrimitive?.contentOrNull
                        ?: tags["natural"]?.jsonPrimitive?.contentOrNull
                        ?: tags["leisure"]?.jsonPrimitive?.contentOrNull,
                    imageUrl = tags["image"]?.jsonPrimitive?.contentOrNull,
                    openingHours = tags["opening_hours"]?.jsonPrimitive?.contentOrNull,
                    fee = tags["fee"]?.jsonPrimitive?.contentOrNull,
                    phone = tags["phone"]?.jsonPrimitive?.contentOrNull
                        ?: tags["contact:phone"]?.jsonPrimitive?.contentOrNull,
                    website = tags["website"]?.jsonPrimitive?.contentOrNull
                        ?: tags["contact:website"]?.jsonPrimitive?.contentOrNull,
                )
            }
        } catch (e: Exception) {
            println("🔌 [Overpass] Parsing error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Maps OSM tags to the Xplore PinType string, matching the backend classification logic.
     */
    private fun classifyOsmTags(tags: JsonObject): String {
        val tourism = tags["tourism"]?.jsonPrimitive?.contentOrNull
        val historic = tags["historic"]?.jsonPrimitive?.contentOrNull
        val amenity = tags["amenity"]?.jsonPrimitive?.contentOrNull
        val natural = tags["natural"]?.jsonPrimitive?.contentOrNull
        val leisure = tags["leisure"]?.jsonPrimitive?.contentOrNull

        return when {
            tourism == "museum" || tourism == "gallery" -> "MUSEUM"
            tourism == "artwork" -> "ARTWORK"
            tourism == "viewpoint" -> "VIEWPOINT"
            tourism == "attraction" -> "ATTRACTION"
            historic != null -> "HISTORIC"
            amenity == "place_of_worship" -> "RELIGIOUS"
            amenity == "theatre" || amenity == "arts_centre" || amenity == "community_centre" -> "CULTURE"
            natural != null -> "NATURE"
            leisure == "park" || leisure == "garden" || leisure == "nature_reserve" -> "NATURE"
            else -> "OTHER"
        }
    }
}


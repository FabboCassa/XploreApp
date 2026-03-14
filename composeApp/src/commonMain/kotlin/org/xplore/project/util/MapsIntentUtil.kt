package org.xplore.project.util

import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.usecase.GenerateAutomatedRouteUseCase.TravelMode

/**
 * Utility to build a Google Maps URL with multiple waypoints.
 *
 * Uses the official Google Maps URL scheme:
 * https://www.google.com/maps/dir/?api=1&destination=LAT,LNG&waypoints=LAT,LNG|LAT,LNG&travelmode=walking
 *
 * This does NOT require an API key — it simply opens the Google Maps
 * app/browser with the route pre-filled. Fully allowed for commercial use.
 */
object MapsIntentUtil {

    /**
     * Maximum number of stops supported by Google Maps URL scheme.
     * Google Maps accepts 1 destination + up to 9 intermediate waypoints = 10 total.
     */
    const val MAX_MAPS_STOPS = 10

    /**
     * Builds a Google Maps directions URL from [startLat]/[startLng] through
     * [waypoints] to the final destination (last waypoint).
     *
     * If the list exceeds [MAX_MAPS_STOPS], only the first 10 stops are used.
     *
     * @param startLat optional start latitude (user position); if null, Google
     *                 Maps will use the device's current location.
     * @param startLng optional start longitude.
     * @param waypoints ordered list of POIs forming the route.
     * @param travelMode travel mode (walking, bicycling, driving).
     * @return the URL string ready to be opened via UriHandler.
     */
    fun buildGoogleMapsUrl(
        startLat: Double? = null,
        startLng: Double? = null,
        waypoints: List<MapPin>,
        travelMode: TravelMode = TravelMode.WALKING,
    ): String? {
        if (waypoints.isEmpty()) return null

        // Google Maps supports max 10 stops total
        val limitedWaypoints = waypoints.take(MAX_MAPS_STOPS)

        val destination = limitedWaypoints.last()
        val intermediateWaypoints = limitedWaypoints.dropLast(1)

        val sb = StringBuilder("https://www.google.com/maps/dir/?api=1")

        // Origin
        if (startLat != null && startLng != null) {
            sb.append("&origin=$startLat,$startLng")
        }

        // Destination (always the last waypoint)
        sb.append("&destination=${destination.latitude},${destination.longitude}")

        // Intermediate waypoints (pipe-separated)
        if (intermediateWaypoints.isNotEmpty()) {
            val waypointsStr = intermediateWaypoints.joinToString("|") {
                "${it.latitude},${it.longitude}"
            }
            sb.append("&waypoints=$waypointsStr")
        }

        // Travel mode
        val mode = when (travelMode) {
            TravelMode.WALKING   -> "walking"
            TravelMode.BICYCLING -> "bicycling"
            TravelMode.DRIVING   -> "driving"
        }
        sb.append("&travelmode=$mode")

        return sb.toString()
    }
}

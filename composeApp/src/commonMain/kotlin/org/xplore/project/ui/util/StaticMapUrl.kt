package org.xplore.project.ui.util

import org.xplore.project.domain.model.SavedRouteWaypoint
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Builds a free OpenStreetMap static map URL (staticmap.openstreetmap.de).
 * No API key needed, no usage limits for reasonable use.
 *
 * Returns just the background map tiles — the route polyline is drawn
 * on top by the Canvas overlay in RoutePreview.
 */
fun buildStaticMapUrl(
    waypoints: List<SavedRouteWaypoint>,
    width: Int = 600,
    height: Int = 300,
): String? {
    if (waypoints.size < 2) return null
    val sorted = waypoints.sortedBy { it.orderIndex }

    val minLat = sorted.minOf { it.latitude }
    val maxLat = sorted.maxOf { it.latitude }
    val minLon = sorted.minOf { it.longitude }
    val maxLon = sorted.maxOf { it.longitude }

    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0
    val zoom = calculateZoom(minLat, maxLat, minLon, maxLon, width, height)

    return "https://staticmap.openstreetmap.de/staticmap.php" +
        "?center=${centerLat},${centerLon}" +
        "&zoom=$zoom" +
        "&size=${width}x${height}" +
        "&maptype=mapnik"
}

/**
 * Calculates the best zoom level to fit the bounding box in the given pixel dimensions.
 * Uses web Mercator projection math.
 */
private fun calculateZoom(
    minLat: Double,
    maxLat: Double,
    minLon: Double,
    maxLon: Double,
    width: Int,
    height: Int,
): Int {
    if (minLat == maxLat && minLon == maxLon) return 15

    val latFraction = (latRad(maxLat) - latRad(minLat)) / kotlin.math.PI
    val lonFraction = (maxLon - minLon) / 360.0

    val latZoom = zoomForFraction(height, TILE_SIZE, latFraction)
    val lonZoom = zoomForFraction(width, TILE_SIZE, lonFraction)

    // Use the smaller zoom (fits both dimensions), add padding margin
    return (min(latZoom, lonZoom) - 1).coerceIn(1, 18)
}

private fun latRad(lat: Double): Double {
    val sinLat = kotlin.math.sin(lat * kotlin.math.PI / 180.0)
    return ln((1 + sinLat) / (1 - sinLat)) / 2.0
}

private fun zoomForFraction(pixels: Int, tileSize: Int, fraction: Double): Int {
    if (fraction <= 0) return 18
    return (ln(pixels.toDouble() / tileSize / fraction) / ln(2.0)).roundToInt()
}

private const val TILE_SIZE = 256

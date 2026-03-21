package org.xplore.project.ui.components
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.CompassButton
import org.maplibre.compose.material3.ScaleBar
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.PinType

/**
 * Interactive map with POI markers colored by type, user location dot,
 * and a centered callout popup on tap.
 *
 * Color Palette (no blue — reserved for user location):
 * - MUSEUM      → Red #D32F2F
 * - ARTWORK     → Orange #E8833A
 * - HISTORIC    → Brown #8D6E63
 * - RELIGIOUS   → Purple #7B1FA2
 * - NATURE      → Green #388E3C
 * - CULTURE     → Pink #E91E90
 * - ATTRACTION  → Golden #F9A825
 * - VIEWPOINT   → Teal #00897B
 * - EVENT       → Deep Orange #E64A19
 * - OTHER       → Grey #78909C
 */
@Composable
fun XploreMap(
    pins: List<MapPin>,
    onPinClick: (MapPin) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    selectedPin: MapPin? = null,
    selectedPinIds: Set<String> = emptySet(),
    initialCameraPosition: CameraPosition? = null,
    onDismissCallout: () -> Unit = {},
    onDetailClick: (String) -> Unit = {},
    onCameraMove: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onAddStop: (MapPin) -> Unit = {},
    routeStops: List<MapPin> = emptyList(),
    routeGeometryJson: String? = null,
    isNavigationActive: Boolean = false,
    routeNextStopIndex: Int = 0,
) {
    val styleUrl = if (isDark)
        "https://tiles.openfreemap.org/styles/dark"
    else
        "https://tiles.openfreemap.org/styles/bright"
    val cameraState = rememberCameraState()
    
    var hasInitializedCamera by remember { mutableStateOf(false) }

    // ── Set initial camera position if provided ──
    LaunchedEffect(initialCameraPosition) {
        if (initialCameraPosition != null && !hasInitializedCamera) {
            cameraState.position = initialCameraPosition
            hasInitializedCamera = true
        }
    }

    var hasCenteredOnUser by remember { mutableStateOf(false) }

    // ── Animate camera to user location on first fix ──
    LaunchedEffect(userLatitude, userLongitude) {
        if (userLatitude != null && userLongitude != null && !hasCenteredOnUser) {
            cameraState.animateTo(
                finalPosition = CameraPosition(
                    target = Position(longitude = userLongitude, latitude = userLatitude),
                    zoom = 14.0,
                ),
            )
            hasCenteredOnUser = true
        }
    }



    // ── Dismiss callout when camera moves ──
    LaunchedEffect(selectedPin) {
        if (selectedPin != null) {
            snapshotFlow { cameraState.position }
                .drop(1)
                .collect { onDismissCallout() }
        }
    }

    // ── Track camera movement to save last position ──
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.position }
            .drop(1)
            .collect { position ->
                onCameraMove(position.target.latitude, position.target.longitude)
            }
    }

    // ── ID-based click resolution ──
    val pinById = remember(pins) { pins.associateBy { it.id } }
    val idRegex = remember { """"id"\s*:\s*"([^"]+)"""".toRegex() }

    val resolveClick: (Any?) -> Unit = { clickedFeatures ->
        val clickedId = try {
            val featureStr = (clickedFeatures as? List<*>)?.firstOrNull()?.toString().orEmpty()
            idRegex.find(featureStr)?.groupValues?.getOrNull(1)
        } catch (_: Exception) { null }
        clickedId?.let { pinById[it] }?.let(onPinClick)
    }

    // ── Group pins by type ──
    val pinsByType = remember(pins) { pins.groupBy { it.type } }

    // ── GeoJSON per type ──
    val jsonByType = remember(pinsByType) {
        pinsByType.mapValues { (_, typePins) -> pinsToGeoJsonString(typePins) }
    }

    val userJson = remember(userLatitude, userLongitude) {
        userLocationGeoJsonString(userLatitude, userLongitude)
    }

    val userDotColor = Color(0xFF2196F3)

    Box(modifier = modifier) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(styleUrl),
            cameraState = cameraState,
            options = MapOptions(ornamentOptions = OrnamentOptions.AllDisabled),
        ) {
            val userSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(userJson))

            // ── User location ──
            CircleLayer(
                id = "user-glow",
                source = userSource,
                radius = const(16.dp),
                color = const(userDotColor.copy(alpha = 0.2f)),
                visible = userLatitude != null && userLongitude != null,
            )
            CircleLayer(
                id = "user-dot",
                source = userSource,
                radius = const(8.dp),
                color = const(userDotColor),
                strokeColor = const(Color.White),
                strokeWidth = const(2.5.dp),
                visible = userLatitude != null && userLongitude != null,
            )

            // ── Selected pins highlight ──
            if (selectedPinIds.isNotEmpty()) {
                val selectedPinsList = remember(pins, selectedPinIds) { pins.filter { it.id in selectedPinIds } }
                val selectedJson = remember(selectedPinsList) { pinsToGeoJsonString(selectedPinsList) }
                val selectedSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(selectedJson))
                
                CircleLayer(
                    id = "poi-selected-highlight",
                    source = selectedSource,
                    radius = const(14.dp),
                    color = const(Color(0xFFFFEB3B)), // Yellow highlight
                    strokeColor = const(Color.Black),
                    strokeWidth = const(2.dp)
                )
            }

            // ── Route polyline — split into current leg (solid) and future legs (transparent) ──
            if (routeStops.size >= 2) {
                // Completed Route - Gray
                val visitedStops = routeStops.take(routeNextStopIndex)
                if (visitedStops.isNotEmpty() && userLatitude != null && userLongitude != null) {
                    val completedCoords = visitedStops.map { it.longitude to it.latitude } + (userLongitude to userLatitude)
                    val completedJson = remember(completedCoords) {
                        val coordsStr = completedCoords.joinToString(",") { (lng, lat) -> "[$lng,$lat]" }
                        """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coordsStr]},"properties":{}}]}"""
                    }
                    val completedSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(completedJson))
                    LineLayer(
                        id = "route-line-completed-casing",
                        source = completedSource,
                        color = const(Color(0xFF9E9E9E).copy(alpha = 0.5f)),
                        width = const(8.dp),
                    )
                    LineLayer(
                        id = "route-line-completed",
                        source = completedSource,
                        color = const(Color(0xFF9E9E9E)),
                        width = const(4.dp),
                    )
                }

                val routeLineJson = routeGeometryJson ?: remember(routeStops) { routeLineGeoJsonString(routeStops) }
                val nextStop = routeStops.getOrNull(routeNextStopIndex)

                val (currentLegJson, futureLegJson) = remember(routeLineJson, nextStop) {
                    splitRouteGeoJson(routeLineJson, nextStop)
                }

                // Current leg — solid blue with casing
                if (currentLegJson != null) {
                    val currentSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(currentLegJson))
                    LineLayer(
                        id = "route-line-current-casing",
                        source = currentSource,
                        color = const(Color(0xFF0D47A1)),
                        width = const(9.dp),
                    )
                    LineLayer(
                        id = "route-line-current",
                        source = currentSource,
                        color = const(Color(0xFF00B0FF)),
                        width = const(4.5.dp),
                    )
                }

                // Future legs — transparent light blue, NO casing
                if (futureLegJson != null) {
                    val futureSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(futureLegJson))
                    LineLayer(
                        id = "route-line-future",
                        source = futureSource,
                        color = const(Color(0xFF81D4FA).copy(alpha = 0.25f)),
                        width = const(2.dp),
                    )
                }
            }

            // ── Route stop markers (3-color by visit status) ──
            if (routeStops.isNotEmpty()) {
                // Visited stops (grey)
                val visitedStops = routeStops.take(routeNextStopIndex)
                if (visitedStops.isNotEmpty()) {
                    val visitedJson = remember(visitedStops) { routeMarkersGeoJsonString(visitedStops) }
                    val visitedSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(visitedJson))
                    CircleLayer(
                        id = "route-stops-visited",
                        source = visitedSource,
                        radius = const(8.dp),
                        color = const(Color(0xFF9E9E9E)),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp),
                    )
                }

                // Next stop (green, emphasised)
                val nextStop = routeStops.getOrNull(routeNextStopIndex)
                if (nextStop != null) {
                    val nextJson = remember(nextStop) { routeMarkersGeoJsonString(listOf(nextStop)) }
                    val nextSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(nextJson))
                    CircleLayer(
                        id = "route-stop-next-glow",
                        source = nextSource,
                        radius = const(20.dp),
                        color = const(Color(0xFF4CAF50).copy(alpha = 0.25f)),
                    )
                    CircleLayer(
                        id = "route-stop-next",
                        source = nextSource,
                        radius = const(12.dp),
                        color = const(Color(0xFF4CAF50)),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp),
                    )
                }

                // Future stops (orange)
                val futureStops = routeStops.drop(routeNextStopIndex + 1)
                if (futureStops.isNotEmpty()) {
                    val futureJson = remember(futureStops) { routeMarkersGeoJsonString(futureStops) }
                    val futureSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(futureJson))
                    CircleLayer(
                        id = "route-stops-future-glow",
                        source = futureSource,
                        radius = const(16.dp),
                        color = const(Color(0xFFE64A19).copy(alpha = 0.25f)),
                    )
                    CircleLayer(
                        id = "route-stops-future",
                        source = futureSource,
                        radius = const(10.dp),
                        color = const(Color(0xFFE64A19)),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.5.dp),
                    )
                }
            }

            // ── Per-type POI layers (hidden during navigation) ──
            if (routeStops.isEmpty()) {
                for ((type, json) in jsonByType) {
                    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(json))
                    val color = pinTypeColor(type)

                    CircleLayer(
                        id = "poi-${type.name.lowercase()}",
                        source = source,
                        radius = const(8.dp),
                        color = const(color),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp),
                        onClick = { features ->
                            resolveClick(features)
                            ClickResult.Consume
                        },
                    )
                }
            }
        }

        // ── Clickable scrim to dismiss callout on tap outside ──
        if (selectedPin != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onDismissCallout() }
                    },
            )
            PinCallout(
                pin = selectedPin,
                onDetailClick = onDetailClick,
                onAddStop = { onAddStop(selectedPin) },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ScaleBar
        ScaleBar(
            metersPerDp = cameraState.metersPerDpAtTarget,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp),
        )

        // Compass
        CompassButton(
            cameraState = cameraState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        )
    }
}

// ── Pin type → color mapping ──
fun pinTypeColor(type: PinType): Color = when (type) {
    PinType.MUSEUM     -> Color(0xFFD32F2F)  // Red
    PinType.ARTWORK    -> Color(0xFFE8833A)  // Orange
    PinType.HISTORIC   -> Color(0xFF8D6E63)  // Brown
    PinType.RELIGIOUS  -> Color(0xFF7B1FA2)  // Purple
    PinType.NATURE     -> Color(0xFF388E3C)  // Green
    PinType.CULTURE    -> Color(0xFFE91E90)  // Pink
    PinType.ATTRACTION -> Color(0xFFF9A825)  // Golden
    PinType.VIEWPOINT  -> Color(0xFF00897B)  // Teal
    PinType.EVENT      -> Color(0xFFE64A19)  // Deep Orange
    PinType.OTHER      -> Color(0xFF78909C)  // Grey
}

// ── GeoJSON String Builders ──

private fun pinsToGeoJsonString(pins: List<MapPin>): String {
    val features = pins.joinToString(",") { pin ->
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${pin.longitude},${pin.latitude}]},"properties":{"id":"${pin.id}","label":"${pin.label.replace("\"", "\\\"")}"}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

private fun userLocationGeoJsonString(lat: Double?, lng: Double?): String {
    if (lat == null || lng == null) {
        return """{"type":"FeatureCollection","features":[]}"""
    }
    return """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[$lng,$lat]},"properties":{}}]}"""
}

private fun routeLineGeoJsonString(stops: List<MapPin>): String {
    val coords = stops.joinToString(",") { "[${it.longitude},${it.latitude}]" }
    return """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]},"properties":{}}]}"""
}

private fun routeMarkersGeoJsonString(stops: List<MapPin>): String {
    val features = stops.mapIndexed { index, pin ->
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${pin.longitude},${pin.latitude}]},"properties":{"index":${index + 1}}}"""
    }.joinToString(",")
    return """{"type":"FeatureCollection","features":[$features]}"""
}

/**
 * Splits a route GeoJSON LineString into two parts at [splitAt] pin location.
 * Returns (currentLeg, futureLeg) as GeoJSON FeatureCollection strings.
 * If splitAt is null, returns the full route as currentLeg and null for futureLeg.
 */
private fun splitRouteGeoJson(
    routeGeoJson: String,
    splitAt: MapPin?,
): Pair<String?, String?> {
    try {
        val coordRegex = """\[(-?\d+\.?\d*),\s*(-?\d+\.?\d*)\]""".toRegex()
        val coords = coordRegex.findAll(routeGeoJson).map {
            it.groupValues[1].toDouble() to it.groupValues[2].toDouble() // lng, lat
        }.toList()

        if (coords.size < 2) return routeGeoJson to null
        if (splitAt == null) return routeGeoJson to null

        var splitIndex = coords.size // default: full route is current leg
        var minDistance = Double.MAX_VALUE

        for (i in coords.indices) {
            val (lng, lat) = coords[i]
            val dist = haversineMeters(lat, lng, splitAt.latitude, splitAt.longitude)
            if (dist < minDistance) {
                minDistance = dist
                splitIndex = i
            }
        }

        fun toGeoJson(pts: List<Pair<Double, Double>>): String? {
            if (pts.size < 2) return null
            val c = pts.joinToString(",") { (lng, lat) -> "[$lng,$lat]" }
            return """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$c]},"properties":{}}]}"""
        }

        // Include the split point in both segments so they connect visually
        val currentCoords = coords.take(splitIndex + 1)
        val futureCoords = coords.drop(splitIndex)

        return toGeoJson(currentCoords) to toGeoJson(futureCoords)
    } catch (_: Exception) {
        return routeGeoJson to null
    }
}

private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val dLat = kotlin.math.PI / 180.0 * (lat2 - lat1)
    val dLng = kotlin.math.PI / 180.0 * (lng2 - lng1)
    val radLat1 = kotlin.math.PI / 180.0 * lat1
    val radLat2 = kotlin.math.PI / 180.0 * lat2
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(radLat1) * kotlin.math.cos(radLat2) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}

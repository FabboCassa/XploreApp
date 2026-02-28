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
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    selectedPin: MapPin? = null,
    onDismissCallout: () -> Unit = {},
) {
    val styleUrl = "https://tiles.openfreemap.org/styles/liberty"
    val cameraState = rememberCameraState()
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

            // ── Per-type POI layers ──
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

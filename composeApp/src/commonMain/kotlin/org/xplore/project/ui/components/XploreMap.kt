package org.xplore.project.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * Interactive map component powered by **MapLibre Compose** + OpenStreetMap tiles.
 *
 * Displays:
 * - POI markers as colored circles (blue=Museum, orange=Artwork, green=Event)
 * - User location as a blue pulsing dot
 * - Custom compass and scale bar
 */
@Composable
fun XploreMap(
    pins: List<MapPin>,
    onPinClick: (MapPin) -> Unit,
    modifier: Modifier = Modifier,
    userLatitude: Double? = null,
    userLongitude: Double? = null,
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

    // ── Pre-compute pin lists (no map-scope needed) ──
    val museumPins = remember(pins) { pins.filter { it.type == PinType.MUSEUM } }
    val artworkPins = remember(pins) { pins.filter { it.type == PinType.ARTWORK } }
    val eventPins = remember(pins) { pins.filter { it.type == PinType.EVENT } }

    // ── Build raw GeoJSON strings (avoids spatial-k serialization issues) ──
    val museumJson = remember(museumPins) { pinsToGeoJsonString(museumPins) }
    val artworkJson = remember(artworkPins) { pinsToGeoJsonString(artworkPins) }
    val eventJson = remember(eventPins) { pinsToGeoJsonString(eventPins) }
    val userJson = remember(userLatitude, userLongitude) {
        userLocationGeoJsonString(userLatitude, userLongitude)
    }

    // ── Colors ──
    val museumColor = Color(0xFF4A90D9)
    val artworkColor = Color(0xFFE8833A)
    val eventColor = Color(0xFF4CAF50)
    val userDotColor = Color(0xFF2196F3)

    Box(modifier = modifier) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(styleUrl),
            cameraState = cameraState,
            options = MapOptions(
                ornamentOptions = OrnamentOptions.AllDisabled
            )
        ) {
            // ══════════════════════════════════════════════════════
            // rememberGeoJsonSource MUST be inside MaplibreMap { }
            // Using GeoJsonData.JsonString to bypass spatial-k
            // polymorphic serializer issues with Feature<Geometry?, ...>
            // ══════════════════════════════════════════════════════

            val museumSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(museumJson))
            val artworkSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(artworkJson))
            val eventSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(eventJson))
            val userSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(userJson))

            // ── User location — outer glow ring ──
            CircleLayer(
                id = "user-location-glow",
                source = userSource,
                radius = const(16.dp),
                color = const(userDotColor.copy(alpha = 0.2f)),
                visible = userLatitude != null && userLongitude != null,
            )

            // ── User location — inner dot ──
            CircleLayer(
                id = "user-location-dot",
                source = userSource,
                radius = const(8.dp),
                color = const(userDotColor),
                strokeColor = const(Color.White),
                strokeWidth = const(2.5.dp),
                visible = userLatitude != null && userLongitude != null,
            )

            // ── Museum markers (Blue) ──
            CircleLayer(
                id = "poi-museums",
                source = museumSource,
                radius = const(8.dp),
                color = const(museumColor),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
                onClick = { _ ->
                    museumPins.firstOrNull()?.let(onPinClick)
                    ClickResult.Consume
                },
            )

            // ── Artwork markers (Orange) ──
            CircleLayer(
                id = "poi-artworks",
                source = artworkSource,
                radius = const(8.dp),
                color = const(artworkColor),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
                onClick = { _ ->
                    artworkPins.firstOrNull()?.let(onPinClick)
                    ClickResult.Consume
                },
            )

            // ── Event markers (Green) ──
            CircleLayer(
                id = "poi-events",
                source = eventSource,
                radius = const(8.dp),
                color = const(eventColor),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
                onClick = { _ ->
                    eventPins.firstOrNull()?.let(onPinClick)
                    ClickResult.Consume
                },
            )
        }

        // ScaleBar
        ScaleBar(
            metersPerDp = cameraState.metersPerDpAtTarget,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        )

        // Compass
        CompassButton(
            cameraState = cameraState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )
    }
}

// ── GeoJSON String Builders ──────────────────────────────────────

/**
 * Build a raw GeoJSON FeatureCollection string from [MapPin] list.
 * Uses raw JSON to avoid spatial-k serialization/polymorphic issues.
 */
private fun pinsToGeoJsonString(pins: List<MapPin>): String {
    val features = pins.joinToString(",") { pin ->
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${pin.longitude},${pin.latitude}]},"properties":{"id":"${pin.id}","label":"${pin.label.replace("\"", "\\\"")}"}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

/**
 * Build a raw GeoJSON FeatureCollection string for the user location dot.
 * Returns an empty collection if coordinates are null.
 */
private fun userLocationGeoJsonString(lat: Double?, lng: Double?): String {
    if (lat == null || lng == null) {
        return """{"type":"FeatureCollection","features":[]}"""
    }
    return """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[$lng,$lat]},"properties":{}}]}"""
}

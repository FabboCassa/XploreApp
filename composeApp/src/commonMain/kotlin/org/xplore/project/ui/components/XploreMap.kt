package org.xplore.project.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.xplore.project.domain.model.MapPin

/**
 * Interactive map component powered by **MapLibre Compose** + OpenStreetMap tiles.
 *
 * ## Architecture Role
 * Drop-in replacement for the previous placeholder. Uses [MaplibreMap] from the
 * `maplibre-compose` library with **OpenFreeMap** tiles (free — no API key required,
 * fully open source, commercially viable).
 *
 * ## Integration Notes
 * - Style: OpenFreeMap "liberty" (light, clean, OSM data).
 * - Pins will be rendered as map annotations in a future step.
 * - For offline support, MapLibre's `OfflineManager` will download tile regions
 *   around the user's location (implemented in a future step).
 *
 * @param pins List of domain model pins to display on the map.
 * @param onPinClick Event callback when a pin is tapped.
 * @param modifier Allows size and layout customization from the parent.
 */
@Composable
fun XploreMap(
    pins: List<MapPin>,
    onPinClick: (MapPin) -> Unit,
    modifier: Modifier = Modifier,
) {
    // OpenFreeMap: free OSM tile server, no API key, no rate limits, MIT-compatible
    val styleUrl = "https://tiles.openfreemap.org/styles/liberty"

    MaplibreMap(
        modifier = modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri(styleUrl),
    )
}

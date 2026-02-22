package org.xplore.project.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.CompassButton
import org.maplibre.compose.material3.ScaleBar
import org.maplibre.compose.style.BaseStyle
import org.xplore.project.domain.model.MapPin

/**
 * Interactive map component powered by **MapLibre Compose** + OpenStreetMap tiles.
 */
@Composable
fun XploreMap(
    pins: List<MapPin>,
    onPinClick: (MapPin) -> Unit,
    modifier: Modifier = Modifier,
) {
    // OpenFreeMap: free OSM tile server, no API key, no rate limits, MIT-compatible
    val styleUrl = "https://tiles.openfreemap.org/styles/liberty"
    
    val cameraState = rememberCameraState()

    Box(modifier = modifier) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(styleUrl),
            cameraState = cameraState,
            // Hide default ornaments so we can place them manually above the bottom bar
            options = MapOptions(
                ornamentOptions = OrnamentOptions.AllDisabled
            )
        )

        // ScaleBar aligned to the bottom-left, just above the bottom bar
        ScaleBar(
            metersPerDp = cameraState.metersPerDpAtTarget,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        )

        // Custom Compass aligned to the bottom-right, just above the bottom bar
        CompassButton(
            cameraState = cameraState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )
    }
}

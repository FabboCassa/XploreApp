package org.xplore.project.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.domain.model.MapPin
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.ic_map
import xploreapp.composeapp.generated.resources.map_placeholder_label
import xploreapp.composeapp.generated.resources.map_placeholder_subtitle

/**
 * Map abstract interface component.
 *
 * ## Architecture Role
 * This component abstracts the underlying Map SDK. Currently, it renders a **Premium Placeholder** with animations.
 *
 * ## Integration Guide
 * To integrate a real map (e.g., Google Maps or Mapbox):
 * 1. Add the CMP Map SDK dependency.
 * 2. Replace the body of this composable with the SDK's map view.
 * 3. Map [MapPin] domain models to the SDK's pin format.
 * 4. Keep the function signature stable to avoid breaking [HomeScreen].
 *
 * @param pins List of domain model pins to display.
 * @param onPinClick Event callback when a pin is tapped.
 */
@Composable
fun XploreMap(
    pins: List<MapPin>,
    onPinClick: (MapPin) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    // Subtle animated gradient to give a "living" feel
    val infiniteTransition = rememberInfiniteTransition(label = "mapGradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradientShift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.background,
                        colorScheme.primaryContainer.copy(alpha = 0.3f),
                        colorScheme.surfaceVariant,
                    ),
                    start = Offset(0f, animatedOffset * 1000f),
                    end = Offset(1000f, (1f - animatedOffset) * 1000f),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Draw subtle grid lines to simulate a map
        val gridColor = colorScheme.outline.copy(alpha = 0.15f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 60.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }

        // Render pin dots (decorative in placeholder mode)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pinColor = Color(0xFFFFD54F)
            val pinShadow = Color(0x44000000)
            pins.forEachIndexed { index, _ ->
                // Distribute pins in a visually pleasing pattern
                val cx = size.width * (0.2f + (index % 3) * 0.3f)
                val cy = size.height * (0.25f + (index / 3) * 0.2f)
                drawCircle(pinShadow, radius = 10f, center = Offset(cx + 2f, cy + 2f))
                drawCircle(pinColor, radius = 8f, center = Offset(cx, cy))
            }
        }

        // Center label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(Res.drawable.ic_map),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = colorScheme.primary.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.map_placeholder_label),
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.map_placeholder_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onBackground.copy(alpha = 0.4f),
            )
        }
    }
}

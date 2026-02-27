package org.xplore.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.xplore.project.domain.model.MapPin

/**
 * Popup card that appears centered on the screen when a map pin is tapped.
 * White rounded rectangle showing name, category, and description.
 * The category badge color matches the pin's type color on the map.
 */
@Composable
fun PinCallout(
    pin: MapPin,
    modifier: Modifier = Modifier,
) {
    val accentColor = pinTypeColor(pin.type)

    Box(
        modifier = modifier
            .widthIn(min = 260.dp, max = 280.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Column {
            // ── Image Header ──
            if (!pin.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = pin.imageUrl,
                    contentDescription = pin.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFEEEEEE)) // Placeholder color
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // ── Category badge ──
                if (!pin.category.isNullOrBlank()) {
                    Text(
                        text = pin.category.uppercase(),
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // ── Name ──
                Text(
                    text = pin.label,
                    color = Color(0xFF1A1A2E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!pin.description.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = pin.description,
                        color = Color(0xFF555555),
                        fontSize = 13.sp,
                        maxLines = 4, // Allow more lines since it's richer text now
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                    )
                }
            } // Close inner Column
        } // Close outer Column
    } // Close Box
} // Close PinCallout function

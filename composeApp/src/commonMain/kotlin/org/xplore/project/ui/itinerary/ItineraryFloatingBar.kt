package org.xplore.project.ui.itinerary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.domain.model.ItineraryStop
import org.xplore.project.ui.components.pinTypeColor
import xploreapp.composeapp.generated.resources.*



/**
 * Floating bottom bar that appears when the user has at least one itinerary stop.
 * Shows the count of stops and two action buttons:
 * - **Avvia Navigazione**: open in-app step-by-step navigation.
 * - **Esporta in Maps**: open Google Maps (max 10 stops).
 */
@Composable
fun ItineraryFloatingBar(
    stops: List<ItineraryStop>,
    isNavigationActive: Boolean = false,
    nextStopIndex: Int = 0,
    nextStopDistanceMeters: Double? = null,
    nextStopDurationSeconds: Double? = null,
    navigationInstruction: String? = null,
    isLoadedFromSaved: Boolean = false,
    onSaveRoute: () -> Unit = {},
    onCancelLoadedRoute: () -> Unit = {},
    onNavigate: () -> Unit,
    onStopNavigation: () -> Unit = {},
    onExport: () -> Unit,
    onClear: () -> Unit,
    onRemoveStop: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = stops.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(16.dp),
        ) {
            if (isNavigationActive) {
                // ── Navigation active mode ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header row
                    Text(
                        text = stringResource(Res.string.itinerary_navigation_active),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A90D9),
                    )

                    Spacer(Modifier.height(4.dp))

                    // Next POI info
                    val nextStop = stops.getOrNull(nextStopIndex)
                    if (nextStop != null) {
                        // POI name
                        Text(
                            text = nextStop.pin.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A2E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(2.dp))

                        // Turn-by-turn instruction
                        if (navigationInstruction != null) {
                            Text(
                                text = navigationInstruction,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF4A90D9),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                        }

                        // Distance & ETA row
                        val infoBuilder = StringBuilder()
                        val progressText = stringResource(
                            Res.string.itinerary_nav_stop_of,
                            nextStopIndex + 1,
                            stops.size,
                        )
                        infoBuilder.append(progressText)

                        if (nextStopDistanceMeters != null) {
                            val distText = if (nextStopDistanceMeters < 1000) {
                                "${nextStopDistanceMeters.toInt()} m"
                            } else {
                                val km = nextStopDistanceMeters / 1000.0
                                "${(kotlin.math.round(km * 10) / 10.0)} km"
                            }
                            infoBuilder.append(" • $distText")
                        }

                        if (nextStopDurationSeconds != null) {
                            val mins = (nextStopDurationSeconds / 60.0).toInt()
                            val durText = if (mins < 1) "< 1 min" else "$mins min"
                            infoBuilder.append(" • $durText")
                        }

                        Text(
                            text = infoBuilder.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF888888),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Single full-width Stop button
                OutlinedButton(
                    onClick = onStopNavigation,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.itinerary_stop_navigation),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            } else {
                // ── Normal itinerary mode ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val stopCountText = if (stops.size == 1)
                        stringResource(Res.string.itinerary_stops_count_single)
                    else
                        stringResource(Res.string.itinerary_stops_count, stops.size)

                    if (isLoadedFromSaved) {
                        TextButton(onClick = onCancelLoadedRoute) {
                            Text(
                                text = stringResource(Res.string.itinerary_cancel_loaded),
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        IconButton(onClick = onSaveRoute, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(Res.string.itinerary_save_route),
                                tint = Color(0xFF4A90D9),
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stopCountText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E),
                        )
                    }

                    Row {
                        TextButton(onClick = onClear) {
                            Text(
                                text = stringResource(Res.string.itinerary_clear),
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Stop chips (scrollable) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stops.forEachIndexed { index, stop ->
                        StopChip(
                            index = index + 1,
                            name = stop.pin.label,
                            color = pinTypeColor(stop.pin.type),
                            onRemove = { onRemoveStop(stop.pin.id) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Navigate Button (primary) ──
                Button(
                    onClick = onNavigate,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90D9),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.itinerary_navigate),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Export to Maps Button (secondary) ──
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF388E3C),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.itinerary_export_maps),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StopChip(
    index: Int,
    name: String,
    color: Color,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF5F5F5))
            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
    ) {
        // Number badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp),
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = Color(0xFFAAAAAA),
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onRemove),
        )
    }
}

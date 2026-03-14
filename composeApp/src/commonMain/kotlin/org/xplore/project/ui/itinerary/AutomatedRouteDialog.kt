package org.xplore.project.ui.itinerary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.domain.model.PinType
import org.xplore.project.domain.usecase.GenerateAutomatedRouteUseCase.RouteConstraint
import org.xplore.project.domain.usecase.GenerateAutomatedRouteUseCase.TravelMode
import xploreapp.composeapp.generated.resources.*

/**
 * Constraint mode chosen by the user in the automated route dialog.
 */
private enum class ConstraintMode { TIME, STOPS }

/**
 * Full-screen dialog for configuring an automated route.
 * The user can choose between:
 * - **Per Tempo**: maximum travel time between stops (visit time excluded).
 * - **Per Quantità**: exact number of stops to visit.
 *
 * Additionally: travel mode and preferred categories.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomatedRouteDialog(
    onDismiss: () -> Unit,
    onGenerate: (constraint: RouteConstraint, travelMode: TravelMode, categories: Set<PinType>) -> Unit,
) {
    var constraintMode by remember { mutableStateOf(ConstraintMode.TIME) }
    var timeSlider by remember { mutableFloatStateOf(120f) } // default 2h
    var stopsSlider by remember { mutableFloatStateOf(10f) } // default 10 stops
    var selectedMode by remember { mutableStateOf(TravelMode.WALKING) }
    var selectedCategories by remember { mutableStateOf(emptySet<PinType>()) }
    var showTimeInfoDialog by remember { mutableStateOf(false) }

    val timeMinutes = timeSlider.toInt()
    val hours = timeMinutes / 60
    val mins = timeMinutes % 60
    val stopsCount = stopsSlider.toInt()

    // ── Info AlertDialog ──
    if (showTimeInfoDialog) {
        AlertDialog(
            onDismissRequest = { showTimeInfoDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.route_dialog_time_info_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(text = stringResource(Res.string.route_dialog_time_info_body))
            },
            confirmButton = {
                TextButton(onClick = { showTimeInfoDialog = false }) {
                    Text("OK")
                }
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.route_dialog_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.close),
                            tint = Color(0xFF888888),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Constraint Mode Toggle ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ConstraintModeChip(
                        label = stringResource(Res.string.route_dialog_constraint_time),
                        selected = constraintMode == ConstraintMode.TIME,
                        onClick = { constraintMode = ConstraintMode.TIME },
                        modifier = Modifier.weight(1f),
                    )
                    ConstraintModeChip(
                        label = stringResource(Res.string.route_dialog_constraint_stops),
                        selected = constraintMode == ConstraintMode.STOPS,
                        onClick = { constraintMode = ConstraintMode.STOPS },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Constraint-specific section ──
                when (constraintMode) {
                    ConstraintMode.TIME -> {
                        // Label + Info icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.route_dialog_time_label),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF555555),
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Info",
                                tint = Color(0xFF4A90D9),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showTimeInfoDialog = true },
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Time slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = timeSlider,
                                onValueChange = { timeSlider = (it / 10f).toInt() * 10f },
                                valueRange = 30f..480f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4A90D9),
                                    activeTrackColor = Color(0xFF4A90D9),
                                    inactiveTrackColor = Color(0xFFE0E0E0),
                                ),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (hours > 0 && mins > 0) "${hours}h ${mins}m"
                                       else if (hours > 0) "${hours}h"
                                       else "${mins}m",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A90D9),
                            )
                        }
                    }

                    ConstraintMode.STOPS -> {
                        Text(
                            text = stringResource(Res.string.route_dialog_stops_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF555555),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = stopsSlider,
                                onValueChange = { stopsSlider = it.toInt().toFloat() },
                                valueRange = 1f..50f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4A90D9),
                                    activeTrackColor = Color(0xFF4A90D9),
                                    inactiveTrackColor = Color(0xFFE0E0E0),
                                ),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "$stopsCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A90D9),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Travel Mode ──
                Text(
                    text = stringResource(Res.string.route_dialog_transport_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555555),
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TravelModeChip(
                        icon = Icons.Filled.DirectionsWalk,
                        label = stringResource(Res.string.route_dialog_transport_walking),
                        selected = selectedMode == TravelMode.WALKING,
                        onClick = { selectedMode = TravelMode.WALKING },
                        modifier = Modifier.weight(1f),
                    )
                    TravelModeChip(
                        icon = Icons.Filled.DirectionsBike,
                        label = stringResource(Res.string.route_dialog_transport_bicycling),
                        selected = selectedMode == TravelMode.BICYCLING,
                        onClick = { selectedMode = TravelMode.BICYCLING },
                        modifier = Modifier.weight(1f),
                    )
                    TravelModeChip(
                        icon = Icons.Filled.DirectionsCar,
                        label = stringResource(Res.string.route_dialog_transport_driving),
                        selected = selectedMode == TravelMode.DRIVING,
                        onClick = { selectedMode = TravelMode.DRIVING },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Categories ──
                Text(
                    text = stringResource(Res.string.route_dialog_categories_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555555),
                )
                Spacer(Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryChip(
                        label = stringResource(Res.string.route_dialog_categories_all),
                        selected = selectedCategories.isEmpty(),
                        onClick = { selectedCategories = emptySet() },
                    )

                    PinType.entries.forEach { type ->
                        CategoryChip(
                            label = pinTypeLabelIt(type),
                            selected = type in selectedCategories,
                            onClick = {
                                selectedCategories = if (type in selectedCategories) {
                                    selectedCategories - type
                                } else {
                                    selectedCategories + type
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Generate Button ──
                Button(
                    onClick = {
                        val constraint = when (constraintMode) {
                            ConstraintMode.TIME -> RouteConstraint.MaxTravelTime(timeMinutes)
                            ConstraintMode.STOPS -> RouteConstraint.MaxStops(stopsCount)
                        }
                        onGenerate(constraint, selectedMode, selectedCategories)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90D9),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = stringResource(Res.string.route_dialog_generate),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

// ── Constraint Mode Chip ────────────────────────────────

@Composable
private fun ConstraintModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF4A90D9) else Color(0xFFF0F2F5),
        animationSpec = tween(200),
        label = "constraintBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFF555555),
        animationSpec = tween(200),
        label = "constraintText",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}

// ── Travel Mode Chip ────────────────────────────────────

@Composable
private fun TravelModeChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF4A90D9) else Color(0xFFF0F2F5),
        animationSpec = tween(200),
        label = "travelBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFF555555),
        animationSpec = tween(200),
        label = "travelContent",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

// ── Category Chip ───────────────────────────────────────

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF4A90D9) else Color(0xFFF0F2F5),
        animationSpec = tween(200),
        label = "catBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFF555555),
        animationSpec = tween(200),
        label = "catText",
    )

    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/**
 * Italian labels for each PinType used in the automated route dialog.
 */
private fun pinTypeLabelIt(type: PinType): String = when (type) {
    PinType.MUSEUM     -> "Musei"
    PinType.ARTWORK    -> "Opere"
    PinType.HISTORIC   -> "Monumenti"
    PinType.RELIGIOUS  -> "Luoghi sacri"
    PinType.NATURE     -> "Natura"
    PinType.CULTURE    -> "Cultura"
    PinType.ATTRACTION -> "Attrazioni"
    PinType.VIEWPOINT  -> "Panorami"
    PinType.EVENT      -> "Eventi"
    PinType.OTHER      -> "Altro"
}

package org.xplore.project.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.*
import kotlin.math.roundToInt

// ── Settings: Radius slider data ─────────────────────────────────

/**
 * Non-proportional discrete radius steps (in km).
 *
 * - 100m → 1km by 100m   (indices 0–9)   ~45% of slider
 * - 1.5km → 5km by 0.5km (indices 10–17) ~36% of slider
 * - 6km → 10km by 1km    (indices 18–22) ~19% of slider  ← red zone
 */
private val RADIUS_STEPS = listOf(
    0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0,
    1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0,
    6.0, 7.0, 8.0, 9.0, 10.0,
)

/** First red-zone step index (6 km). Everything before this is blue. */
private const val RED_START_INDEX = 18

private val BLUE_COLOR = Color(0xFF4A90D9)
private val RED_COLOR = Color(0xFFD32F2F)

private fun formatRadius(km: Double): String = when {
    km < 1.0 -> "${(km * 1000).toInt()}m"
    km == km.toLong().toDouble() -> "${km.toInt()} km"       // 1 km, 2 km …
    else -> "${"%.1f".format(km)} km"                         // 1.5 km, 2.5 km …
}

private fun closestIndex(km: Double): Int {
    var best = 0
    var bestDiff = Double.MAX_VALUE
    RADIUS_STEPS.forEachIndexed { i, v ->
        val diff = kotlin.math.abs(v - km)
        if (diff < bestDiff) { bestDiff = diff; best = i }
    }
    return best
}

// ── Settings Dialog ──────────────────────────────────────────────

@Composable
fun SettingsDialog(
    searchRadiusKm: Double,
    radiusAverages: Map<Double, Long>,
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    selectedLanguage: org.xplore.project.data.local.LanguageMode,
    onLanguageChange: (org.xplore.project.data.local.LanguageMode) -> Unit,
    onRadiusChange: (Double) -> Unit,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
) {
    // Local state for the slider, so we only apply when the user clicks "Applica"
    var localRadiusKm by remember { mutableStateOf(searchRadiusKm) }

    val currentIndex = closestIndex(localRadiusKm)
    val isRedZone = currentIndex >= RED_START_INDEX

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.80f) // Slightly taller to fit the new button
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.close),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Search Radius section ──
                Text(
                    text = stringResource(Res.string.settings_search_radius),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(4.dp))

                // Current value label
                Text(
                    text = formatRadius(localRadiusKm),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRedZone) RED_COLOR else BLUE_COLOR,
                )

                if (isRedZone) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.settings_warning_slow),
                        style = MaterialTheme.typography.bodySmall,
                        color = RED_COLOR,
                    )
                }

                // Estimated loading time from backend metrics
                val avgMs = radiusAverages[localRadiusKm]
                if (avgMs != null) {
                    Spacer(Modifier.height(4.dp))
                    val seconds = "%.1f".format(avgMs / 1000.0)
                    Text(
                        text = stringResource(Res.string.settings_estimated_time, seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRedZone) RED_COLOR.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Custom dual-color slider ──
                RadiusSlider(
                    currentIndex = currentIndex,
                    onIndexChange = { idx -> localRadiusKm = RADIUS_STEPS[idx] },
                )

                // Min / max labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatRadius(RADIUS_STEPS.first()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatRadius(RADIUS_STEPS.last()),
                        style = MaterialTheme.typography.labelSmall,
                        color = RED_COLOR.copy(alpha = 0.7f),
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                // ── Placeholder: Data Sources ──
                SettingsToggleRow(
                    label = stringResource(Res.string.settings_data_sources),
                    checked = true,
                    onCheckedChange = { /* placeholder */ },
                )

                Spacer(Modifier.height(12.dp))

                // ── Notifications ──
                SettingsToggleRow(
                    label = stringResource(Res.string.settings_notifications),
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsToggle,
                )

                Spacer(Modifier.height(12.dp))

                // ── Language Selector ──
                Text(
                    text = stringResource(Res.string.settings_language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))

                var languageExpanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedButton(
                        onClick = { languageExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = selectedLanguage.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false },
                    ) {
                        org.xplore.project.data.local.LanguageMode.entries.forEach { mode ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.displayName,
                                        fontWeight = if (mode == selectedLanguage)
                                            FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onLanguageChange(mode)
                                    languageExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                // ── Apply Radius Button ──
                Button(
                    onClick = {
                        onRadiusChange(localRadiusKm)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    enabled = localRadiusKm != searchRadiusKm
                ) {
                    Text(
                        text = stringResource(Res.string.settings_btn_apply),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Logout ──
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = stringResource(Res.string.settings_btn_logout),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Dual-color radius slider ─────────────────────────────────────

/**
 * A slider with a blue-to-red track. The track is blue up to [RED_START_INDEX]
 * and red beyond it. The thumb snaps to discrete [RADIUS_STEPS] values.
 */
@Composable
private fun RadiusSlider(
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
) {
    val maxIndex = RADIUS_STEPS.size - 1
    // Fraction of the track that is blue (up to the last blue step)
    val blueFraction = (RED_START_INDEX.toFloat() - 0.5f) / maxIndex.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        // ── Custom two-color track behind the slider ──
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(horizontal = 10.dp) // align with Slider's thumb padding
        ) {
            val h = size.height
            val w = size.width
            val splitX = w * blueFraction
            val r = CornerRadius(h / 2f, h / 2f)

            // Blue portion
            drawRoundRect(
                color = BLUE_COLOR,
                topLeft = Offset.Zero,
                size = Size(splitX, h),
                cornerRadius = r,
            )
            // Red portion
            drawRoundRect(
                color = RED_COLOR,
                topLeft = Offset(splitX, 0f),
                size = Size(w - splitX, h),
                cornerRadius = r,
            )
        }

        // ── Interactive slider (transparent track, visible thumb) ──
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { onIndexChange(it.roundToInt().coerceIn(0, maxIndex)) },
            valueRange = 0f..maxIndex.toFloat(),
            steps = maxIndex - 1,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = if (currentIndex >= RED_START_INDEX) RED_COLOR else BLUE_COLOR,
                activeTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            ),
        )
    }
}

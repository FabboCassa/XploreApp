package org.xplore.project.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.compose.BindLocationTrackerEffect
import dev.icerock.moko.geo.compose.LocationTrackerAccuracy
import dev.icerock.moko.geo.compose.rememberLocationTrackerFactory
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.ui.chat.ChatScreen
import org.xplore.project.ui.components.XploreBottomNavBar
import org.xplore.project.ui.components.XploreFilterChips
import org.xplore.project.ui.components.XploreMap
import org.xplore.project.ui.components.XploreSearchBar
import org.xplore.project.ui.profile.ProfileScreen
import xploreapp.composeapp.generated.resources.*

/**
 * The main container screen that hosts the Bottom Navigation and switches
 * between Map, Chat, and Profile content.
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Location Tracking (composable-level, platform-aware) ──
    val locationTrackerFactory = rememberLocationTrackerFactory(LocationTrackerAccuracy.Best)
    val locationTracker: LocationTracker = remember { locationTrackerFactory.createLocationTracker() }

    BindLocationTrackerEffect(locationTracker = locationTracker)

    // Collect location updates and forward to ViewModel
    LaunchedEffect(locationTracker) {
        try {
            locationTracker.startTracking()
            locationTracker.getLocationsFlow()
                .distinctUntilChanged()
                .collect { latLng ->
                    viewModel.onLocationUpdate(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                    )
                }
        } catch (_: Exception) {
            viewModel.onLocationUnavailable()
        }
    }

    Scaffold(
        bottomBar = {
            XploreBottomNavBar(
                selectedIndex = uiState.selectedNavIndex,
                onItemSelected = viewModel::onNavItemSelected,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState.selectedNavIndex) {
                0 -> MapContent(uiState = uiState, viewModel = viewModel)
                1 -> ChatScreen()
                2 -> ProfileScreen(
                    onLogout = {
                        viewModel.logout()
                        onLogout()
                    },
                    onClearMapCache = { viewModel.clearMapCache() },
                )
            }
        }
    }

    // ── Settings Dialog (rendered above everything) ──
    if (uiState.isSettingsOpen) {
        SettingsDialog(
            searchRadiusKm = uiState.searchRadiusKm,
            onRadiusChange = viewModel::updateSearchRadius,
            onDismiss = viewModel::closeSettings,
            onLogout = {
                viewModel.closeSettings()
                viewModel.logout()
                onLogout()
            },
        )
    }
}

/**
 * Map tab content — extracted for clarity.
 */
@Composable
private fun MapContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        XploreMap(
            pins = uiState.pins,
            onPinClick = { pin -> viewModel.onPinSelected(pin) },
            modifier = Modifier.fillMaxSize(),
            userLatitude = uiState.userLatitude,
            userLongitude = uiState.userLongitude,
            selectedPin = uiState.selectedPin,
            onDismissCallout = { viewModel.onDismissCallout() },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 4.dp),
        ) {
            XploreSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onClear = viewModel::onClearSearch,
            )

            Spacer(Modifier.height(12.dp))

            XploreFilterChips(
                filters = uiState.filters,
                onFilterClick = viewModel::onFilterSelected,
                onSettingsClick = viewModel::openSettings,
            )

            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(4.dp),
                        color = Color(0xFF4A90D9),
                        trackColor = Color(0xFF4A90D9).copy(alpha = 0.15f),
                    )

                    if (uiState.loadingStatusText != null) {
                        Text(
                            text = uiState.loadingStatusText.asString(),
                            fontSize = 12.sp,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

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
private fun SettingsDialog(
    searchRadiusKm: Double,
    onRadiusChange: (Double) -> Unit,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
) {
    val currentIndex = closestIndex(searchRadiusKm)
    val isRedZone = currentIndex >= RED_START_INDEX

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.72f)
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
                            contentDescription = "Close",
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
                    text = formatRadius(searchRadiusKm),
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

                Spacer(Modifier.height(8.dp))

                // ── Custom dual-color slider ──
                RadiusSlider(
                    currentIndex = currentIndex,
                    onIndexChange = { idx -> onRadiusChange(RADIUS_STEPS[idx]) },
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

                // ── Placeholder: Notifications ──
                SettingsToggleRow(
                    label = stringResource(Res.string.settings_notifications),
                    checked = false,
                    onCheckedChange = { /* placeholder */ },
                )

                Spacer(Modifier.weight(1f))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                // ── Logout ──
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
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

package org.xplore.project.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedButton
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
            radiusAverages = uiState.radiusAverages,
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


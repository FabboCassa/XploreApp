package org.xplore.project.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.compose.BindLocationTrackerEffect
import dev.icerock.moko.geo.compose.LocationTrackerAccuracy
import dev.icerock.moko.geo.compose.rememberLocationTrackerFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.ui.chat.ChatScreen
import org.xplore.project.ui.components.XploreBottomNavBar
import org.xplore.project.ui.profile.ProfileScreen

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

    // ── Filter Dialog (rendered above everything) ──
    if (uiState.isFilterDialogOpen) {
        FilterDialog(
            filters = uiState.filters,
            onApply = viewModel::applyFilters,
            onDismiss = viewModel::closeFilterDialog,
        )
    }
}


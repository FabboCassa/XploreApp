package org.xplore.project.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.compose.BindLocationTrackerEffect
import dev.icerock.moko.geo.compose.LocationTrackerAccuracy
import dev.icerock.moko.geo.compose.rememberLocationTrackerFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.ui.chat.ChatScreen
import org.xplore.project.ui.components.XploreBottomNavBar
import org.xplore.project.ui.components.XploreFilterChips
import org.xplore.project.ui.components.XploreMap
import org.xplore.project.ui.components.XploreSearchBar
import org.xplore.project.ui.profile.ProfileScreen

/**
 * The main container screen that hosts the Bottom Navigation and switches
 * between Map, Chat, and Profile content.
 *
 * ## UI Structure
 * Uses a [Scaffold] with a bottom [XploreBottomNavBar].
 * Content is switched based on [HomeUiState.selectedNavIndex]:
 * - **Tab 0 (Map)**: Full-screen map with search bar and filter chips overlay.
 * - **Tab 1 (Chat)**: AI Chat interface.
 * - **Tab 2 (Profile)**: User profile and settings.
 *
 * ## Location Tracking
 * LocationTracker is created at the Composable level using moko-geo-compose
 * because it requires platform-specific context (applicationContext on Android).
 * Location updates are forwarded to [HomeViewModel] via [HomeViewModel.onLocationUpdate].
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
}

/**
 * Map tab content — extracted for clarity.
 * Shows the map background with search bar and filter chips overlay.
 */
@Composable
private fun MapContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // ── Layer 1: Map (full-bleed behind everything) ──
        XploreMap(
            pins = uiState.pins,
            onPinClick = { pin -> viewModel.onPinSelected(pin) },
            modifier = Modifier.fillMaxSize(),
            userLatitude = uiState.userLatitude,
            userLongitude = uiState.userLongitude,
            selectedPin = uiState.selectedPin,
            onDismissCallout = { viewModel.onDismissCallout() },
        )

        // ── Layer 2: Top overlay (search + filters + loading bar) ──
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
            )

            // ── Loading bar + status text ──
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


package org.xplore.project.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.compose.BindLocationTrackerEffect
import dev.icerock.moko.geo.compose.LocationTrackerAccuracy
import dev.icerock.moko.geo.compose.rememberLocationTrackerFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.data.local.ThemeMode
import org.xplore.project.ui.app.AppViewModel
import org.xplore.project.ui.community.CommunityScreen
import org.xplore.project.ui.components.XploreBottomNavBar
import org.xplore.project.ui.profile.ProfileScreen
import org.xplore.project.ui.util.LocalNotificationPermissionRequester
import org.xplore.project.ui.util.PendingDeepLink

/**
 * The main container screen that hosts the Bottom Navigation and switches
 * between Map, Chat, and Profile content.
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToPoiDetail: (String) -> Unit = {},
    onNavigateToGroupDetail: (String) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    appViewModel: AppViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by appViewModel.themeMode.collectAsState()
    var openFriendRequests by remember { mutableStateOf(false) }

    // Handle notification deep links (e.g. tap on "friend_request" notification)
    val deepLinkEvent by PendingDeepLink.event.collectAsState()
    LaunchedEffect(deepLinkEvent) {
        if (deepLinkEvent == "open_friend_requests") {
            viewModel.onNavItemSelected(2)
            openFriendRequests = true
            PendingDeepLink.consume()
        }
    }
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

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
            val hasPendingNotification by viewModel.hasPendingNotification.collectAsState()
            XploreBottomNavBar(
                selectedIndex = uiState.selectedNavIndex,
                onItemSelected = viewModel::onNavItemSelected,
                profileHasBadge = hasPendingNotification,
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
                0 -> MapContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDetailClick = onNavigateToPoiDetail,
                    isDark = isDark,
                )
                1 -> CommunityScreen(
                    onNavigateToGroupDetail = onNavigateToGroupDetail,
                )
                2 -> ProfileScreen(
                    onLogout = {
                        viewModel.logout()
                        onLogout()
                    },
                    onClearMapCache = { viewModel.clearMapCache() },
                    openFriendRequests = openFriendRequests,
                    onFriendRequestsConsumed = { openFriendRequests = false },
                )
            }
        }
    }

    // ── Settings Dialog (rendered above everything) ──
    if (uiState.isSettingsOpen) {
        val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
        val permissionRequester = LocalNotificationPermissionRequester.current

        SettingsDialog(
            searchRadiusKm = uiState.searchRadiusKm,
            radiusAverages = uiState.radiusAverages,
            notificationsEnabled = notificationsEnabled,
            onNotificationsToggle = { enabled ->
                viewModel.onNotificationsToggle(enabled, permissionRequester)
            },
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

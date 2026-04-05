package org.xplore.project.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.compose.BindLocationTrackerEffect
import dev.icerock.moko.geo.compose.LocationTrackerAccuracy
import dev.icerock.moko.geo.compose.rememberLocationTrackerFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.data.local.ThemeMode
import xploreapp.composeapp.generated.resources.*
import org.xplore.project.ui.app.AppViewModel
import org.xplore.project.ui.community.CommunityScreen
import org.xplore.project.ui.components.XploreBottomNavBar
import org.xplore.project.ui.profile.ProfileScreen
import org.xplore.project.ui.util.LocalNotificationPermissionRequester
import org.xplore.project.ui.util.PendingDeepLink
import org.xplore.project.ui.itinerary.AutomatedRouteDialog
import androidx.compose.ui.platform.LocalUriHandler

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
    val uriHandler = LocalUriHandler.current

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
                selectedIndex = uiState.selectedTab,
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
            when (uiState.selectedTab) {
                0 -> MapContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDetailClick = onNavigateToPoiDetail,
                    isDark = isDark,
                    onExportItinerary = {
                        val url = viewModel.buildExportUrl()
                        if (url != null) {
                            uriHandler.openUri(url)
                        }
                    },
                    onNavigateItinerary = viewModel::startNavigation,
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
                    onOpenMapSettings = viewModel::openSettings,
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
        val currentLanguage by appViewModel.languageMode.collectAsState()

        SettingsDialog(
            searchRadiusKm = uiState.searchRadiusKm,
            radiusAverages = uiState.radiusAverages,
            notificationsEnabled = notificationsEnabled,
            onNotificationsToggle = { enabled ->
                viewModel.onNotificationsToggle(enabled, permissionRequester)
            },
            selectedLanguage = currentLanguage,
            onLanguageChange = appViewModel::setLanguageMode,
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

    // ── Automated Route Dialog ──
    if (uiState.isAutomatedRouteDialogOpen) {
        AutomatedRouteDialog(
            onDismiss = viewModel::closeAutomatedRouteDialog,
            onGenerate = viewModel::generateAutomatedRoute,
        )
    }

    // ── Save Route Dialog ──
    if (uiState.saveRouteDialogOpen) {
        SaveRouteNameDialog(
            onConfirm = { name, desc -> viewModel.saveCurrentRoute(name, desc) },
            onDismiss = viewModel::closeSaveRouteDialog,
        )
    }

}

@Composable
private fun SaveRouteNameDialog(
    onConfirm: (name: String, description: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.route_save_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.route_save_dialog_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.route_save_dialog_description_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description.ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(Res.string.route_save_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.route_save_dialog_cancel))
            }
        },
    )
}

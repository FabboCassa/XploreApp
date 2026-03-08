package org.xplore.project.ui.community.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.compose.BindLocationTrackerEffect
import dev.icerock.moko.geo.compose.LocationTrackerAccuracy
import dev.icerock.moko.geo.compose.rememberLocationTrackerFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.ui.components.XploreMap
import xploreapp.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionMapScreen(
    compId: String,
    // In a real app we'd pass or load the rule IDs dynamically from the API based on compId.
    // For this demonstration, we'll assume it receives the rule POI IDs.
    allowedPoiIds: List<String>, 
    onBack: () -> Unit,
    onNavigateToPoiDetail: (String) -> Unit,
    viewModel: CompetitionMapViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(compId, allowedPoiIds) {
        viewModel.loadCompetitionMap(compId, allowedPoiIds)
    }

    // ── Location Tracking ──
    val locationTrackerFactory = rememberLocationTrackerFactory(LocationTrackerAccuracy.Best)
    val locationTracker: LocationTracker = remember { locationTrackerFactory.createLocationTracker() }

    BindLocationTrackerEffect(locationTracker = locationTracker)

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
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.competition_map_open)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            XploreMap(
                pins = uiState.filteredPins, // Only show POIs relevant to competition
                onPinClick = viewModel::onPinSelected,
                modifier = Modifier.fillMaxSize(),
                userLatitude = uiState.userLatitude,
                userLongitude = uiState.userLongitude,
                selectedPin = uiState.selectedPin,
                onDismissCallout = viewModel::onDismissCallout,
                onDetailClick = onNavigateToPoiDetail
            )
            
            // Note: If we had a requirement to "Save" these to the user's home screen,
            // we'd add another button here that triggers a shared preference or local db save.
        }
    }
}

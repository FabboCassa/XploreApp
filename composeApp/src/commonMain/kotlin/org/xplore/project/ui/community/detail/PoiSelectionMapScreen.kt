package org.xplore.project.ui.community.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
fun PoiSelectionMapScreen(
    groupId: String,
    onBackWithSelection: (List<String>) -> Unit, // Navigate back and pass selection
    viewModel: PoiSelectionMapViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                title = { Text(stringResource(Res.string.competition_map_select_places)) },
                navigationIcon = {
                    IconButton(onClick = { onBackWithSelection(uiState.selectedPoiIds.toList()) }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Only show save button if there's a selection (or show total selected)
                    TextButton(
                        onClick = { onBackWithSelection(uiState.selectedPoiIds.toList()) }
                    ) {
                        Text(
                            text = stringResource(Res.string.competition_map_save_selection, uiState.selectedPoiIds.size),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
            // Reusing XploreMap.
            XploreMap(
                pins = uiState.pins,
                // Instead of opening detail view, clicking a pin toggles it
                onPinClick = { pin -> viewModel.togglePoiSelection(pin.id) },
                modifier = Modifier.fillMaxSize(),
                userLatitude = uiState.userLatitude,
                userLongitude = uiState.userLongitude,
                selectedPin = null, // No detailed callout needed here
                selectedPinIds = uiState.selectedPoiIds,
                onDismissCallout = { },
                onDetailClick = { }
            )
            
            // Show a pill indicator of how many pins are selected
            if (uiState.selectedPoiIds.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.competition_selected_places_count, uiState.selectedPoiIds.size),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

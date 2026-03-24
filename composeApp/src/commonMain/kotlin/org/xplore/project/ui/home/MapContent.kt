package org.xplore.project.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.remember
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.spatialk.geojson.Position
import org.xplore.project.ui.components.XploreFilterChips
import org.xplore.project.ui.components.XploreMap
import org.xplore.project.ui.components.XploreSearchBar
import org.xplore.project.ui.itinerary.ItineraryFloatingBar
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.search_no_results
import xploreapp.composeapp.generated.resources.search_searching
import xploreapp.composeapp.generated.resources.itinerary_automated_route

/**
 * Map tab content — extracted for clarity.
 */
@Composable
fun MapContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    onDetailClick: (String) -> Unit,
    isDark: Boolean = false,
    onExportItinerary: () -> Unit = {},
    onNavigateItinerary: () -> Unit = {},
) {
    // ── Build initial camera position if saved ──
    val initialCamera = remember(uiState.initialCameraLat, uiState.initialCameraLng) {
        if (uiState.initialCameraLat != null && uiState.initialCameraLng != null) {
            CameraPosition(
                target = Position(
                    longitude = uiState.initialCameraLng,
                    latitude = uiState.initialCameraLat
                ),
                zoom = 14.0
            )
        } else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        XploreMap(
            pins = uiState.pins,
            onPinClick = viewModel::onPinSelected,
            isDark = isDark,
            userLatitude = uiState.userLatitude,
            userLongitude = uiState.userLongitude,
            selectedPin = uiState.selectedPin,
            initialCameraPosition = initialCamera,
            onDismissCallout = viewModel::onDismissCallout,
            onDetailClick = onDetailClick,
            onCameraMove = viewModel::onMapCameraMove,
            onAddStop = viewModel::addItineraryStop,
            routeStops = if (uiState.isNavigationActive || uiState.isLoadedFromSaved) {
                uiState.itineraryStops.map { it.pin }
            } else emptyList(),
            routeGeometryJson = uiState.routeInfo?.geometryJson,
            isNavigationActive = uiState.isNavigationActive,
            routeNextStopIndex = uiState.nextStopIndex,
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(
                    WindowInsets.statusBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(top = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XploreSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onClear = viewModel::onClearSearch,
                    modifier = Modifier.weight(1f)
                )

                androidx.compose.material3.Button(
                    onClick = viewModel::openAutomatedRouteDialog,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Route,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Filter chips row ──
            XploreFilterChips(
                filters = uiState.filters,
                onFilterClick = viewModel::onFilterSelected,
                onMoreFiltersClick = viewModel::openFilterDialog,
            )

            // ── Search indicator ──
            AnimatedVisibility(
                visible = uiState.isSearching,
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
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    )
                    Text(
                        text = stringResource(Res.string.search_searching),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }

            // ── No results label ──
            AnimatedVisibility(
                visible = !uiState.isSearching
                        && uiState.searchQuery.length >= 2
                        && uiState.searchResults.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = stringResource(Res.string.search_no_results),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            // ── POI loading indicator ──
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
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    )

                    if (uiState.loadingStatusText != null) {
                        Text(
                            text = uiState.loadingStatusText.asString(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // ── Itinerary Floating Bar + Snackbar (bottom) ──
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Snackbar sits directly above the floating bar
            if (uiState.itinerarySnackbar != null) {
                Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                ) {
                    Text(text = uiState.itinerarySnackbar)
                }
            }

            ItineraryFloatingBar(
                stops = uiState.itineraryStops,
                isNavigationActive = uiState.isNavigationActive,
                nextStopIndex = uiState.nextStopIndex,
                nextStopDistanceMeters = uiState.routeInfo?.nextLegDistanceMeters,
                nextStopDurationSeconds = uiState.routeInfo?.nextLegDurationSeconds,
                navigationInstruction = uiState.routeInfo?.nextInstruction,
                isLoadedFromSaved = uiState.isLoadedFromSaved,
                onSaveRoute = viewModel::openSaveRouteDialog,
                onCancelLoadedRoute = viewModel::clearItinerary,
                onNavigate = onNavigateItinerary,
                onStopNavigation = viewModel::stopNavigation,
                onExport = onExportItinerary,
                onClear = viewModel::clearItinerary,
                onRemoveStop = viewModel::removeItineraryStop,
            )
        }
    }
}

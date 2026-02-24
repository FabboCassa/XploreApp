package org.xplore.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.PinType
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.MuseumRepository
import xploreapp.composeapp.generated.resources.*
import xploreapp.composeapp.generated.resources.filter_artworks
import xploreapp.composeapp.generated.resources.filter_events
import xploreapp.composeapp.generated.resources.filter_museums
import xploreapp.composeapp.generated.resources.filter_nearby

/**
 * ViewModel for the Home screen (Map view).
 *
 * ## Presentation Layer
 * This class acts as the **State Holder** for the screen. It follows the **Unidirectional Data Flow (UDF)** pattern:
 * - **State**: Exposes [HomeUiState] via [StateFlow] which the UI observes.
 * - **Events**: Public methods (e.g., [onSearchQueryChanged]) handle user intent.
 * - **Business Logic**: Filters pins based on active chips and search queries.
 *
 * ## Dependencies
 * @param museumRepository Injected via Koin. Used to fetch and cache POIs.
 * @param authRepository Injected via Koin. Used for logout.
 */
class HomeViewModel(
    private val museumRepository: MuseumRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            filters = defaultFilters(),
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ── User Actions ──────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, selectedPin = null) }
    }

    fun onFilterSelected(filterId: String) {
        _uiState.update { state ->
            state.copy(
                filters = state.filters.map { chip ->
                    if (chip.id == filterId) chip.copy(selected = !chip.selected)
                    else chip
                }
            )
        }
        // Re-filter with current location
        val state = _uiState.value
        if (state.userLatitude != null && state.userLongitude != null) {
            loadPins(state.userLatitude, state.userLongitude)
        }
    }

    fun onNavItemSelected(index: Int) {
        _uiState.update { it.copy(selectedNavIndex = index, selectedPin = null) }
    }

    fun onClearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    fun onPinSelected(pin: MapPin) {
        _uiState.update { it.copy(selectedPin = pin) }
    }

    fun onDismissCallout() {
        _uiState.update { it.copy(selectedPin = null) }
    }

    fun logout() {
        authRepository.logout()
    }

    /**
     * Clears the locally cached map data (POIs).
     * Does NOT affect user login, progress, or saved places.
     */
    fun clearMapCache() {
        viewModelScope.launch {
            museumRepository.clearMapCache()
            _uiState.update { it.copy(pins = emptyList()) }
        }
    }

    // ── Location Updates (called from Composable layer) ───────────

    /**
     * Called by the Composable layer when a new GPS location is received.
     * Triggers a POI fetch for the user's current area.
     */
    fun onLocationUpdate(latitude: Double, longitude: Double) {
        val isFirstFix = _uiState.value.userLatitude == null
        _uiState.update {
            it.copy(
                userLatitude = latitude,
                userLongitude = longitude,
                locationPermissionGranted = true,
            )
        }
        // Load POIs on first location fix
        if (isFirstFix) {
            loadPins(latitude, longitude)
        }
    }

    /**
     * Called when location permission is denied or location is unavailable.
     */
    fun onLocationUnavailable() {
        _uiState.update { it.copy(locationPermissionGranted = false) }
    }

    // ── Data Loading ──────────────────────────────────────────────

    private fun loadPins(lat: Double, lon: Double, radiusKm: Double = 3.0) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loadingStatusText = org.xplore.project.ui.util.UiText.Resource(
                        Res.string.loading_poi_search
                    )
                )
            }
            try {
                val allPins = museumRepository.getMapPins(lat, lon, radiusKm)

                _uiState.update {
                    it.copy(loadingStatusText = org.xplore.project.ui.util.UiText.Resource(
                        Res.string.loading_poi_found,
                        allPins.size
                    ))
                }

                val activeFilters = _uiState.value.filters
                    .filter { it.selected }
                    .map { it.id }

                val filteredPins = if (activeFilters.isEmpty()) {
                    allPins
                } else {
                    allPins.filter { pin ->
                        when (pin.type) {
                            PinType.MUSEUM -> "museums" in activeFilters
                            PinType.ARTWORK -> "artworks" in activeFilters
                            PinType.EVENT -> "events" in activeFilters
                            else -> true
                        }
                    }
                }

                // Small delay so the user can read the count
                kotlinx.coroutines.delay(800)

                _uiState.update {
                    it.copy(pins = filteredPins, isLoading = false, loadingStatusText = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingStatusText = null,
                        errorMessage = e.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    // ── Default Filters ───────────────────────────────────────────

    private fun defaultFilters() = listOf(
        FilterChipData(id = "museums", labelRes = Res.string.filter_museums),
        FilterChipData(id = "artworks", labelRes = Res.string.filter_artworks),
        FilterChipData(id = "events", labelRes = Res.string.filter_events),
        FilterChipData(id = "nearby", labelRes = Res.string.filter_nearby),
    )
}

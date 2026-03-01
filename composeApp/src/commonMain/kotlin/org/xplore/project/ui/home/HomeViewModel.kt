package org.xplore.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.PinType
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.MuseumRepository
import org.xplore.project.data.remote.RadiusMetricsRemoteDataSource
import org.xplore.project.data.local.TokenManager
import xploreapp.composeapp.generated.resources.*

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
    private val metricsDataSource: RadiusMetricsRemoteDataSource,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            filters = defaultFilters(),
        )
    )
    private var searchJob: Job? = null
    private var loadPinsJob: Job? = null
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Restore radius from persistent settings
        val savedRadius = tokenManager.searchRadiusKm
        _uiState.update { it.copy(searchRadiusKm = savedRadius) }

        // Preload radius loading averages from backend
        viewModelScope.launch {
            val averages = metricsDataSource.getLoadingAverages()
            _uiState.update { it.copy(radiusAverages = averages) }
        }
    }

    // ── User Actions ──────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, selectedPin = null) }

        // Cancel any in-flight search
        searchJob?.cancel()

        if (query.length < 2) {
            // Query too short — restore regular pins
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            reapplyFilters()
            return
        }

        // Debounce 300ms then search
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(query)
        }
    }

    fun onFilterSelected(filterId: String) {
        _uiState.update { state ->
            state.copy(
                filters = state.filters.map { chip ->
                    if (chip.id == filterId) chip.copy(selected = !chip.selected)
                    else chip
                },
            )
        }
        // Re-filter locally — NO network call needed
        reapplyFilters()
    }

    fun onNavItemSelected(index: Int) {
        _uiState.update { it.copy(selectedNavIndex = index, selectedPin = null) }
    }

    fun onClearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
        // Restore regular filtered pins
        reapplyFilters()
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

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
        // Load averages from backend in background
        viewModelScope.launch {
            val averages = metricsDataSource.getLoadingAverages()
            _uiState.update { it.copy(radiusAverages = averages) }
        }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun openFilterDialog() {
        _uiState.update { it.copy(isFilterDialogOpen = true) }
    }

    fun closeFilterDialog() {
        _uiState.update { it.copy(isFilterDialogOpen = false) }
    }

    /**
     * Applies the filter selection from the dialog.
     * Receives the full list of updated [FilterChipData] with their new selection state.
     */
    fun applyFilters(updatedFilters: List<FilterChipData>) {
        _uiState.update { it.copy(filters = updatedFilters, isFilterDialogOpen = false) }
        // Re-filter locally — NO network call needed
        reapplyFilters()
    }

    fun updateSearchRadius(radiusKm: Double) {
        val currentRadius = tokenManager.searchRadiusKm
        val isIncrease = radiusKm > currentRadius

        tokenManager.searchRadiusKm = radiusKm
        tokenManager.radiusSetAtMs = Clock.System.now().toEpochMilliseconds()

        _uiState.update { it.copy(searchRadiusKm = radiusKm) }
        val state = _uiState.value
        val lat = state.userLatitude ?: return
        val lon = state.userLongitude ?: return

        if (isIncrease) {
            // Radius increased — cached data is for a smaller area.
            // Clear the cache so we force a full network fetch for the new area.
            viewModelScope.launch {
                museumRepository.clearMapCache()
                loadPinsInternal(lat, lon, allowNetworkRefresh = true)
            }
        } else {
            // Radius decreased — cached data already covers this area, just re-filter locally.
            loadPinsInternal(lat, lon, allowNetworkRefresh = false)
        }
    }

    /**
     * Clears the locally cached map data (POIs).
     * Does NOT affect user login, progress, or saved places.
     */
    fun clearMapCache() {
        viewModelScope.launch {
            museumRepository.clearMapCache()
            _uiState.update { it.copy(allLoadedPins = emptyList(), pins = emptyList()) }
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

        // Prune SQLDelight cache if we've been at the same/smaller radius for > 30 mins
        val now = Clock.System.now().toEpochMilliseconds()
        val setAt = tokenManager.radiusSetAtMs
        if (setAt > 0 && (now - setAt) > 30 * 60 * 1000) {
            viewModelScope.launch {
                museumRepository.pruneCacheOutsideRadius(
                    lat = latitude,
                    lon = longitude,
                    radiusKm = _uiState.value.searchRadiusKm,
                )
                // Update timestamp so we don't spam the DB pruning process
                tokenManager.radiusSetAtMs = now
            }
        }

        // Load POIs on first location fix
        if (isFirstFix) {
            loadPinsInternal(latitude, longitude, allowNetworkRefresh = true)
        }
    }

    /**
     * Called when location permission is denied or location is unavailable.
     */
    fun onLocationUnavailable() {
        _uiState.update { it.copy(locationPermissionGranted = false) }
    }

    // ── Data Loading ──────────────────────────────────────────────

    /**
     * Fetches POIs from the repository and stores them in [HomeUiState.allLoadedPins].
     * Then applies current filters to produce [HomeUiState.pins].
     *
     * Cancels any previous in-flight loadPins coroutine to avoid race conditions.
     */
    private fun loadPinsInternal(lat: Double, lon: Double, allowNetworkRefresh: Boolean = true) {
        // Cancel any previous load to avoid overlapping updates
        loadPinsJob?.cancel()

        val radiusKm = _uiState.value.searchRadiusKm
        loadPinsJob = viewModelScope.launch {
            _uiState.update {
                // Build loading text with estimated time if available
                val avgMs = it.radiusAverages[radiusKm]
                val loadingText = if (avgMs != null) {
                    val sec = "%.1f".format(avgMs / 1000.0)
                    org.xplore.project.ui.util.UiText.DynamicString(
                        "Ricerca punti di interesse… (~${sec}s)"
                    )
                } else {
                    org.xplore.project.ui.util.UiText.Resource(
                        Res.string.loading_poi_search
                    )
                }
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loadingStatusText = loadingText,
                )
            }
            try {
                val startMs = Clock.System.now().toEpochMilliseconds()
                val allPins = museumRepository.getMapPins(lat, lon, radiusKm, allowNetworkRefresh)
                val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

                // Fire-and-forget: submit timing metric to backend
                viewModelScope.launch {
                    metricsDataSource.postLoadingTime(radiusKm, elapsedMs)
                }

                _uiState.update {
                    it.copy(loadingStatusText = org.xplore.project.ui.util.UiText.Resource(
                        Res.string.loading_poi_found,
                        allPins.size
                    ))
                }

                // Store ALL pins, then apply filters
                _uiState.update { it.copy(allLoadedPins = allPins) }
                reapplyFilters()

                // Small delay so the user can read the count
                delay(800)

                _uiState.update {
                    it.copy(isLoading = false, loadingStatusText = null)
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

    // ── Client-side filtering ─────────────────────────────────────

    /**
     * Applies the currently selected filters to [HomeUiState.allLoadedPins]
     * and updates [HomeUiState.pins]. No network call — purely local.
     */
    private fun reapplyFilters() {
        val state = _uiState.value
        val allPins = state.allLoadedPins
        val activeFilters = state.filters
            .filter { it.selected }
            .map { it.id }

        val filteredPins = if (activeFilters.isEmpty()) {
            allPins
        } else {
            allPins.filter { pin ->
                when (pin.type) {
                    PinType.MUSEUM     -> "museums" in activeFilters
                    PinType.ARTWORK    -> "artworks" in activeFilters
                    PinType.EVENT      -> "events" in activeFilters
                    PinType.HISTORIC   -> "historic" in activeFilters
                    PinType.RELIGIOUS  -> "religious" in activeFilters
                    PinType.NATURE     -> "nature" in activeFilters
                    PinType.CULTURE    -> "culture" in activeFilters
                    PinType.ATTRACTION -> "attractions" in activeFilters
                    PinType.VIEWPOINT  -> "viewpoints" in activeFilters
                    PinType.OTHER      -> "other" in activeFilters
                }
            }
        }

        _uiState.update { it.copy(pins = filteredPins) }
    }

    // ── Search ────────────────────────────────────────────────────

    private fun performSearch(query: String) {
        val state = _uiState.value
        val lat = state.userLatitude ?: return
        val lon = state.userLongitude ?: return
        val radiusKm = state.searchRadiusKm

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = museumRepository.searchPois(query, lat, lon, radiusKm)
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        pins = results,
                        isSearching = false,
                        selectedPin = results.firstOrNull(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = e.message ?: "Search failed",
                    )
                }
            }
        }
    }

    // ── Default Filters ───────────────────────────────────────────

    private fun defaultFilters() = listOf(
        FilterChipData(id = "museums",     labelRes = Res.string.filter_museums),
        FilterChipData(id = "artworks",    labelRes = Res.string.filter_artworks),
        FilterChipData(id = "events",      labelRes = Res.string.filter_events),
        FilterChipData(id = "historic",    labelRes = Res.string.filter_historic),
        FilterChipData(id = "religious",   labelRes = Res.string.filter_religious),
        FilterChipData(id = "nature",      labelRes = Res.string.filter_nature),
        FilterChipData(id = "culture",     labelRes = Res.string.filter_culture),
        FilterChipData(id = "attractions", labelRes = Res.string.filter_attractions),
        FilterChipData(id = "viewpoints",  labelRes = Res.string.filter_viewpoints),
        FilterChipData(id = "other",       labelRes = Res.string.filter_other),
    )
}

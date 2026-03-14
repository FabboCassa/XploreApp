package org.xplore.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.jetbrains.compose.resources.getString
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.PinType
import org.xplore.project.domain.model.GroupInviteStatus
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.CommunityRepository
import org.xplore.project.domain.repository.FriendRepository
import org.xplore.project.domain.repository.MuseumRepository
import org.xplore.project.data.remote.RadiusMetricsRemoteDataSource
import org.xplore.project.data.local.AppPreferences
import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.remote.OsrmRoutingService
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
@OptIn(kotlin.time.ExperimentalTime::class)
class HomeViewModel(
    private val museumRepository: MuseumRepository,
    private val authRepository: AuthRepository,
    private val metricsDataSource: RadiusMetricsRemoteDataSource,
    private val tokenManager: TokenManager,
    private val appPreferences: AppPreferences,
    private val friendRepository: FriendRepository,
    private val communityRepository: CommunityRepository,
    private val osrmRoutingService: OsrmRoutingService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            filters = defaultFilters(),
        )
    )
    private var searchJob: Job? = null
    private var loadPinsJob: Job? = null
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Emits navigation commands from external triggers (e.g. notification tap)
    private val _navigationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    fun navigateTo(event: String) {
        _navigationEvent.tryEmit(event)
    }

    init {
        // Restore base radius
        val savedRadius = tokenManager.searchRadiusKm
        _uiState.update { it.copy(searchRadiusKm = savedRadius) }

        // Restore last map position if available
        val lastLat = tokenManager.lastMapLatitude
        val lastLng = tokenManager.lastMapLongitude
        if (lastLat != null && lastLng != null) {
            _uiState.update { it.copy(
                initialCameraLat = lastLat,
                initialCameraLng = lastLng
            ) }
        }

        // Preload radius loading averages from backend
        viewModelScope.launch {
            val averages = metricsDataSource.getLoadingAverages()
            _uiState.update { it.copy(radiusAverages = averages) }
        }

        // Load pending badge count on startup
        refreshPendingNotificationBadge()

        // Auto-refresh badge every 15s so it updates after accept/reject
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                refreshPendingNotificationBadge()
            }
        }
    }

    // ── Map State ──────────────────────────────────────────────────
    fun onMapCameraMove(latitude: Double, longitude: Double) {
        tokenManager.lastMapLatitude = latitude
        tokenManager.lastMapLongitude = longitude
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
        if (index == 2) refreshPendingNotificationBadge()
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

    // ── Notifications ──────────────────────────────────────────────

    val notificationsEnabled: StateFlow<Boolean> = appPreferences.notificationsEnabledFlow

    private val _hasPendingNotification = MutableStateFlow(false)
    val hasPendingNotification: StateFlow<Boolean> = _hasPendingNotification.asStateFlow()

    /**
     * Loads pending friend requests and group invites count.
     * Sets [hasPendingNotification] to true if at least one is pending.
     * Called on init and when the user navigates to the Profile tab.
     */
    fun refreshPendingNotificationBadge() {
        viewModelScope.launch {
            val hasPendingFriendRequests = friendRepository.getPendingRequests()
                .getOrNull()
                ?.isNotEmpty() == true
            val hasPendingGroupInvites = runCatching {
                communityRepository.getMyGroupInvites()
                    .any { it.status == GroupInviteStatus.Pending }
            }.getOrDefault(false)
            _hasPendingNotification.value = hasPendingFriendRequests || hasPendingGroupInvites
        }
    }

    fun onNotificationsToggle(enabled: Boolean, requestPermission: () -> Unit) {
        if (enabled) {
            requestPermission()
        } else {
            appPreferences.notificationsEnabled = false
        }
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
        if (setAt > 0L && (now - setAt) > 30L * 60 * 1000) {
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

        // ── Live navigation: check proximity to next stop ──
        checkNavigationProgress(latitude, longitude)
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
                    val sec = (kotlin.math.round(avgMs / 100.0) / 10.0).toString()
                    org.xplore.project.ui.util.UiText.Resource(
                        Res.string.loading_poi_search_estimated, sec
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
                val fallbackMsg = getString(Res.string.error_unknown)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingStatusText = null,
                        errorMessage = e.message ?: fallbackMsg,
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
                val fallbackMsg = getString(Res.string.error_search_failed)
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = e.message ?: fallbackMsg,
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

    // ── Itinerary ─────────────────────────────────────────────

    private val routeUseCase = org.xplore.project.domain.usecase.GenerateAutomatedRouteUseCase()

    fun addItineraryStop(pin: MapPin) {
        val state = _uiState.value
        if (state.itineraryStops.any { it.pin.id == pin.id }) {
            viewModelScope.launch {
                _uiState.update { it.copy(itinerarySnackbar = getString(Res.string.itinerary_stop_already_added)) }
                delay(2000)
                _uiState.update { it.copy(itinerarySnackbar = null) }
            }
            return
        }
        val stop = org.xplore.project.domain.model.ItineraryStop(pin = pin)
        _uiState.update { it.copy(itineraryStops = it.itineraryStops + stop) }
        viewModelScope.launch {
            _uiState.update { it.copy(itinerarySnackbar = getString(Res.string.itinerary_stop_added)) }
            delay(2000)
            _uiState.update { it.copy(itinerarySnackbar = null) }
        }
    }

    fun removeItineraryStop(pinId: String) {
        _uiState.update { it.copy(itineraryStops = it.itineraryStops.filter { s -> s.pin.id != pinId }) }
    }

    fun clearItinerary() {
        routeFetchJob?.cancel()
        _uiState.update { it.copy(itineraryStops = emptyList(), isNavigationActive = false, routeGeometryJson = null, nextStopIndex = 0) }
    }

    fun openAutomatedRouteDialog() {
        _uiState.update { it.copy(isAutomatedRouteDialogOpen = true) }
    }

    fun closeAutomatedRouteDialog() {
        _uiState.update { it.copy(isAutomatedRouteDialogOpen = false) }
    }

    private var routeFetchJob: Job? = null

    fun startNavigation() {
        val state = _uiState.value
        val stops = state.itineraryStops
        if (stops.isEmpty()) return

        _uiState.update { it.copy(isNavigationActive = true, nextStopIndex = 0) }
        fetchRouteFromCurrentPosition()
    }

    fun stopNavigation() {
        routeFetchJob?.cancel()
        _uiState.update { it.copy(isNavigationActive = false, routeGeometryJson = null, nextStopIndex = 0) }
    }

    /**
     * Fetches the OSRM route from the user's current position through
     * remaining stops (from [nextStopIndex] onward).
     */
    private fun fetchRouteFromCurrentPosition() {
        routeFetchJob?.cancel()
        val state = _uiState.value
        val lat = state.userLatitude ?: return
        val lng = state.userLongitude ?: return
        val remainingStops = state.itineraryStops.drop(state.nextStopIndex)
        if (remainingStops.isEmpty()) {
            // All stops visited
            _uiState.update { it.copy(routeGeometryJson = null) }
            return
        }

        // Create a virtual "user position" pin as the route origin
        val userPin = MapPin(
            id = "__user_origin__",
            label = "",
            latitude = lat,
            longitude = lng,
            type = PinType.OTHER,
        )
        val waypoints = listOf(userPin) + remainingStops.map { it.pin }

        routeFetchJob = viewModelScope.launch {
            val geometry = osrmRoutingService.fetchRouteGeometry(waypoints)
            _uiState.update { it.copy(routeGeometryJson = geometry) }
        }
    }

    /**
     * Called on every location update during active navigation.
     * Checks if the user is within ~50m of the next stop.
     * If so, advances to the next stop and re-fetches the route.
     */
    private fun checkNavigationProgress(latitude: Double, longitude: Double) {
        val state = _uiState.value
        if (!state.isNavigationActive) return

        val stops = state.itineraryStops
        val nextIndex = state.nextStopIndex
        if (nextIndex >= stops.size) return

        val nextStop = stops[nextIndex].pin
        val distanceMeters = haversineDistance(
            lat1 = latitude, lng1 = longitude,
            lat2 = nextStop.latitude, lng2 = nextStop.longitude,
        )

        if (distanceMeters < 50.0) {
            // Stop reached — advance to next
            val newIndex = nextIndex + 1
            if (newIndex >= stops.size) {
                // All stops visited — navigation complete
                viewModelScope.launch {
                    _uiState.update { it.copy(
                        nextStopIndex = newIndex,
                        routeGeometryJson = null,
                        itinerarySnackbar = "Percorso completato!",
                    ) }
                    delay(3000)
                    _uiState.update { it.copy(
                        isNavigationActive = false,
                        nextStopIndex = 0,
                        itinerarySnackbar = null,
                    ) }
                }
            } else {
                _uiState.update { it.copy(nextStopIndex = newIndex) }
                fetchRouteFromCurrentPosition()
            }
        }
    }

    /**
     * Haversine distance between two lat/lng points, in meters.
     */
    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0 // Earth radius in meters
        val dLat = kotlin.math.PI / 180.0 * (lat2 - lat1)
        val dLng = kotlin.math.PI / 180.0 * (lng2 - lng1)
        val radLat1 = kotlin.math.PI / 180.0 * lat1
        val radLat2 = kotlin.math.PI / 180.0 * lat2
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(radLat1) * kotlin.math.cos(radLat2) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    fun generateAutomatedRoute(
        constraint: org.xplore.project.domain.usecase.GenerateAutomatedRouteUseCase.RouteConstraint,
        travelMode: org.xplore.project.domain.usecase.GenerateAutomatedRouteUseCase.TravelMode,
        categories: Set<org.xplore.project.domain.model.PinType>,
    ) {
        val state = _uiState.value
        val lat = state.userLatitude ?: return
        val lng = state.userLongitude ?: return

        val params = org.xplore.project.domain.usecase.GenerateAutomatedRouteUseCase.Params(
            allPins = state.allLoadedPins,
            startLat = lat,
            startLng = lng,
            constraint = constraint,
            travelMode = travelMode,
            selectedCategories = categories,
        )

        val result = routeUseCase.execute(params)
        _uiState.update {
            it.copy(
                itineraryStops = result.stops,
                isAutomatedRouteDialogOpen = false,
            )
        }

        viewModelScope.launch {
            val snackbarMessage = when {
                result.stops.isEmpty() -> getString(Res.string.route_dialog_no_results)
                result.infoMessage != null -> result.infoMessage
                else -> null
            }

            if (snackbarMessage != null) {
                _uiState.update { it.copy(itinerarySnackbar = snackbarMessage) }
                delay(4000)
                _uiState.update { it.copy(itinerarySnackbar = null) }
            }
        }
    }

    /**
     * Builds a Google Maps URL from the current itinerary and returns it,
     * so the UI layer can open it via UriHandler.
     */
    fun buildExportUrl(): String? {
        val state = _uiState.value
        val pins = state.itineraryStops.map { it.pin }
        // Don't pass origin — let Google Maps use the device's live GPS
        return org.xplore.project.util.MapsIntentUtil.buildGoogleMapsUrl(
            waypoints = pins,
        )
    }

    fun dismissItinerarySnackbar() {
        _uiState.update { it.copy(itinerarySnackbar = null) }
    }
}

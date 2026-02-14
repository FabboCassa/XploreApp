package org.xplore.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xplore.project.domain.model.PinType
import org.xplore.project.domain.repository.MuseumRepository
import xploreapp.composeapp.generated.resources.Res
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
 * @param museumRepository Injected via Koin. Used to fetch the raw list of museums/pins.
 */
class HomeViewModel(
    private val museumRepository: MuseumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            filters = defaultFilters(),
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPins()
    }

    // ── User Actions ──────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
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
        // Re-filter pins based on active filters
        loadPins()
    }

    fun onNavItemSelected(index: Int) {
        _uiState.update { it.copy(selectedNavIndex = index) }
    }

    fun onClearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    // ── Data Loading ──────────────────────────────────────────────

    private fun loadPins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val allPins = museumRepository.getMapPins()
                val activeFilters = _uiState.value.filters
                    .filter { it.selected }
                    .map { it.id }

                val filteredPins = if (activeFilters.isEmpty()) {
                    allPins // No filter active = show all
                } else {
                    allPins.filter { pin ->
                        when (pin.type) {
                            PinType.MUSEUM -> "museums" in activeFilters
                            PinType.ARTWORK -> "artworks" in activeFilters
                            PinType.EVENT -> "events" in activeFilters
                        }
                    }
                }

                _uiState.update {
                    it.copy(pins = filteredPins, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
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

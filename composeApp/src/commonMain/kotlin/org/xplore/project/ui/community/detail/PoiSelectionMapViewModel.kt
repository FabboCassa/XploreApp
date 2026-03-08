package org.xplore.project.ui.community.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xplore.project.data.local.MapPinLocalDataSource
import org.xplore.project.domain.model.MapPin

data class PoiSelectionMapUiState(
    val pins: List<MapPin> = emptyList(),
    val selectedPoiIds: Set<String> = emptySet(),
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
)

class PoiSelectionMapViewModel(
    private val localDataSource: MapPinLocalDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoiSelectionMapUiState())
    val uiState: StateFlow<PoiSelectionMapUiState> = _uiState.asStateFlow()

    init {
        loadPins()
    }

    private fun loadPins() {
        viewModelScope.launch {
            val cachedPins = localDataSource.getCachedPins()
            _uiState.update { it.copy(pins = cachedPins) }
        }
    }

    fun togglePoiSelection(poiId: String) {
        _uiState.update { state ->
            val currentSelected = state.selectedPoiIds.toMutableSet()
            if (currentSelected.contains(poiId)) {
                currentSelected.remove(poiId)
            } else {
                currentSelected.add(poiId)
            }
            state.copy(selectedPoiIds = currentSelected)
        }
    }

    fun onLocationUpdate(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                userLatitude = latitude,
                userLongitude = longitude
            )
        }
    }
}

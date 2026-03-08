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

data class CompetitionMapUiState(
    val compId: String = "",
    val allPins: List<MapPin> = emptyList(),
    val filteredPins: List<MapPin> = emptyList(), // Only those tied to the competition
    val userLatitude: Double = 41.8902, 
    val userLongitude: Double = 12.4922,
    val selectedPin: MapPin? = null
)

class CompetitionMapViewModel(
    private val localDataSource: MapPinLocalDataSource
    // communityRepository would be used here to fetch competition details/rules
    // to know exactly which POIs are allowed. For this step we simulate filtering based
    // on a list of POI IDs passed, or fetched.
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompetitionMapUiState())
    val uiState: StateFlow<CompetitionMapUiState> = _uiState.asStateFlow()

    fun loadCompetitionMap(compId: String, allowedPoiIds: List<String>) {
        _uiState.update { it.copy(compId = compId) }
        
        viewModelScope.launch {
            val cachedPins = localDataSource.getCachedPins()
            val filtered = cachedPins.filter { allowedPoiIds.contains(it.id) }
            _uiState.update { 
                it.copy(
                    allPins = cachedPins,
                    filteredPins = filtered
                ) 
            }
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
    
    fun onPinSelected(pin: MapPin) {
        _uiState.update { it.copy(selectedPin = pin) }
    }

    fun onDismissCallout() {
        _uiState.update { it.copy(selectedPin = null) }
    }
}

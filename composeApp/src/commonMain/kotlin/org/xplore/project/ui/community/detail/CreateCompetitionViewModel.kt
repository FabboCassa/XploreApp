package org.xplore.project.ui.community.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xplore.project.data.remote.dto.CompetitionRuleRequest
import org.xplore.project.domain.repository.CommunityRepository

data class CreateCompetitionUiState(
    val groupId: String = "",
    val name: String = "",
    val type: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val pointsPerPlace: String = "1",
    val selectedPoiIds: List<String> = emptyList(), // Store the selected POIs from map
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class CreateCompetitionViewModel(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCompetitionUiState())
    val uiState: StateFlow<CreateCompetitionUiState> = _uiState.asStateFlow()

    fun setGroupId(groupId: String) {
        _uiState.update { it.copy(groupId = groupId) }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateType(type: Int) {
        _uiState.update { it.copy(type = type) }
    }

    fun updateStartDate(date: String) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun updateEndDate(date: String) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun updatePoints(points: String) {
        _uiState.update { it.copy(pointsPerPlace = points) }
    }

    // This will be called when returning from the POI Selection Map
    fun updateSelectedPois(poiIds: List<String>) {
        _uiState.update { it.copy(selectedPoiIds = poiIds) }
    }

    fun saveCompetition() {
        val state = _uiState.value
        if (state.name.isBlank()) return

        val pts = state.pointsPerPlace.toIntOrNull() ?: 1

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // If standard most places, or list with no POI selected, fall back to "any visit"
                val rules = if (state.selectedPoiIds.isEmpty()) {
                    listOf(
                        CompetitionRuleRequest(
                            actionType = 0, // VisitPlace generally
                            pointsAwarded = pts
                        )
                    )
                } else {
                    // One rule per specific POI
                    state.selectedPoiIds.map { poiId ->
                        CompetitionRuleRequest(
                            actionType = 0, 
                            targetPlaceId = poiId,
                            pointsAwarded = pts
                        )
                    }
                }

                communityRepository.createCompetition(
                    groupId = state.groupId,
                    name = state.name,
                    type = state.type,
                    startDate = state.startDate.ifBlank { null },
                    endDate = state.endDate.ifBlank { null },
                    rules = rules
                )
                
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Errore sconosciuto") }
            }
        }
    }
}

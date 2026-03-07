package org.xplore.project.ui.community.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xplore.project.data.remote.dto.CompetitionRuleRequest
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.CommunityRepository

class GroupDetailViewModel(
    private val communityRepository: CommunityRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null

    fun loadGroup(groupId: String) {
        currentGroupId = groupId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Try to load user ID to see if admin
            var userId: String? = null
            authRepository.getCurrentUser().onSuccess { user ->
                userId = user.id
            }

            try {
                val detail = communityRepository.getGroupDetail(groupId)
                val competitions = communityRepository.getCompetitions(groupId)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        groupDetail = detail,
                        currentUserId = userId,
                        competitions = competitions
                    )
                }
                
                // If there's an active competition, auto-select the first one
                competitions.firstOrNull { it.isActive }?.id?.let { activeId ->
                    selectCompetition(activeId)
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load group details"
                    )
                }
            }
        }
    }

    fun selectCompetition(compId: String) {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(selectedCompetitionId = compId, activeCompetitionLeaderboard = null) }
            try {
                val leaderboard = communityRepository.getCompetitionLeaderboard(groupId, compId)
                _uiState.update { it.copy(activeCompetitionLeaderboard = leaderboard) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun createCompetition(
        name: String,
        type: Int,
        startDate: String?,
        endDate: String?,
        rules: List<CompetitionRuleRequest>,
        onSuccess: () -> Unit
    ) {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                communityRepository.createCompetition(groupId, name, type, startDate, endDate, rules)
                closeCreateCompetitionDialog()
                loadGroup(groupId) // Reload to get new competitions
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = e.message) 
                }
            }
        }
    }

    fun openCreateCompetitionDialog() {
        _uiState.update { it.copy(isCreateCompetitionDialogOpen = true) }
    }

    fun closeCreateCompetitionDialog() {
        _uiState.update { it.copy(isCreateCompetitionDialogOpen = false) }
    }

    fun leaveGroup(onSuccess: () -> Unit) {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            try {
                communityRepository.leaveGroup(groupId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // Admin Action: Delete
    fun deleteGroup(onSuccess: () -> Unit) {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            try {
                communityRepository.deleteGroup(groupId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // Admin Action: Change Visibility
    fun changeVisibility(accessType: Int, password: String?) {
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                communityRepository.changeGroupVisibility(groupId, accessType, password)
                closeChangeVisibilityDialog()
                loadGroup(groupId) // Reload to get updated visibility
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = e.message) 
                }
            }
        }
    }

    fun openChangeVisibilityDialog() {
        _uiState.update { it.copy(isChangeVisibilityDialogOpen = true) }
    }

    fun closeChangeVisibilityDialog() {
        _uiState.update { it.copy(isChangeVisibilityDialogOpen = false) }
    }

    fun openDeleteConfirmation() {
        _uiState.update { it.copy(isDeleteConfirmationOpen = true) }
    }

    fun closeDeleteConfirmation() {
        _uiState.update { it.copy(isDeleteConfirmationOpen = false) }
    }
}

package org.xplore.project.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xplore.project.data.local.TokenManager
import org.xplore.project.domain.repository.CommunityRepository

/**
 * ViewModel for the Community screen.
 *
 * Manages groups, join/leave actions, search, and the explorer leaderboard.
 */
class CommunityViewModel(
    private val communityRepository: CommunityRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState(isGuest = tokenManager.isGuest))
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadData()
    }

    /** Loads all groups, user's groups, and leaderboard. */
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load groups (may fail independently)
            var allGroups = emptyList<org.xplore.project.domain.model.Group>()
            var myGroups = emptyList<org.xplore.project.domain.model.Group>()
            try {
                allGroups = communityRepository.getAllGroups()
                if (!tokenManager.isGuest && tokenManager.isLoggedIn) {
                    myGroups = communityRepository.getMyGroups()
                }
            } catch (e: Exception) {
                println("CommunityVM: Failed to load groups: ${e.message}")
            }

            // Load leaderboard independently so it works even when groups fail
            var leaderboard = emptyList<org.xplore.project.domain.model.LeaderboardEntry>()
            try {
                leaderboard = communityRepository.getLeaderboard()
                println("CommunityVM: Leaderboard loaded with ${leaderboard.size} entries")
            } catch (e: Exception) {
                println("CommunityVM: Failed to load leaderboard: ${e.message}")
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    allGroups = allGroups,
                    myGroups = myGroups,
                    leaderboard = leaderboard,
                )
            }
        }
    }

    // ── Dialogs ──

    fun openCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = true) }
    }

    fun closeCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false) }
    }

    fun openJoinDialog() {
        _uiState.update { it.copy(isJoinDialogOpen = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeJoinDialog() {
        _uiState.update { it.copy(isJoinDialogOpen = false, searchQuery = "", searchResults = emptyList()) }
    }

    // ── Search ──

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // debounce
            try {
                val results = communityRepository.searchGroups(query)
                _uiState.update { it.copy(searchResults = results) }
            } catch (_: Exception) {
                // Silently ignore search errors
            }
        }
    }

    // ── Group Actions ──

    fun createGroup(name: String, description: String?, accessType: Int, password: String?) {
        viewModelScope.launch {
            try {
                communityRepository.createGroup(name, description, null, accessType, password)
                closeCreateDialog()
                loadData()
            } catch (e: Exception) {
                println("CommunityVM Create Error: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun joinGroup(groupId: String, password: String? = null) {
        viewModelScope.launch {
            try {
                communityRepository.joinGroup(groupId, password)
                closeJoinDialog()
                loadData()
            } catch (e: Exception) {
                println("CommunityVM Join Error: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            try {
                communityRepository.leaveGroup(groupId)
                loadData()
            } catch (e: Exception) {
                println("CommunityVM Leave Error: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}

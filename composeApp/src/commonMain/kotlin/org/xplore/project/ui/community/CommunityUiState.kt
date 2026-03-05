package org.xplore.project.ui.community

import org.xplore.project.domain.model.Group
import org.xplore.project.domain.model.LeaderboardEntry

/**
 * UI state for the Community screen.
 */
data class CommunityUiState(
    val isLoading: Boolean = false,
    val myGroups: List<Group> = emptyList(),
    val allGroups: List<Group> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val errorMessage: String? = null,
    val isCreateDialogOpen: Boolean = false,
    val isJoinDialogOpen: Boolean = false,
    val isGuest: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Group> = emptyList(),
)

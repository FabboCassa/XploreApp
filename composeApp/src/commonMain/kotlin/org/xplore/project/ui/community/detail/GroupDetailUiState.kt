package org.xplore.project.ui.community.detail

import org.xplore.project.domain.model.Competition
import org.xplore.project.domain.model.CompetitionLeaderboardEntry
import org.xplore.project.domain.model.GroupDetail

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val groupDetail: GroupDetail? = null,
    val errorMessage: String? = null,
    val currentUserId: String? = null,
    val isChangeVisibilityDialogOpen: Boolean = false,
    val isDeleteConfirmationOpen: Boolean = false,
    val competitions: List<Competition> = emptyList(),
    val activeCompetitionLeaderboard: List<CompetitionLeaderboardEntry>? = null,
    val selectedCompetitionId: String? = null,
    val isCreateCompetitionDialogOpen: Boolean = false,
)

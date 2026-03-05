package org.xplore.project.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * The main Community screen, replacing the old Chat tab.
 *
 * Vertical layout:
 * 1. Header with title + login prompt for guests
 * 2. "My Groups" with "+" (create) and "→" (join) icon buttons
 * 3. Global Explorer Leaderboard
 */
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ──
        CommunityHeader(isGuest = uiState.isGuest)

        // ── My Groups ──
        MyGroupsSection(
            groups = uiState.myGroups,
            isGuest = uiState.isGuest,
            onCreateGroup = viewModel::openCreateDialog,
            onJoinGroup = viewModel::openJoinDialog,
            onLeaveGroup = viewModel::leaveGroup,
        )

        // ── Leaderboard ──
        LeaderboardSection(entries = uiState.leaderboard)
    }

    // ── Dialogs ──
    if (uiState.isCreateDialogOpen) {
        CreateGroupDialog(
            onDismiss = viewModel::closeCreateDialog,
            onCreate = viewModel::createGroup,
        )
    }

    if (uiState.isJoinDialogOpen) {
        JoinGroupDialog(
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            myGroupIds = uiState.myGroups.map { it.id }.toSet(),
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onJoinGroup = viewModel::joinGroup,
            onDismiss = viewModel::closeJoinDialog,
        )
    }
}

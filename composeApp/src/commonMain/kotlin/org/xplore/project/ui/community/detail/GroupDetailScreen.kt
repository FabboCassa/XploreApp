package org.xplore.project.ui.community.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.domain.model.GroupRole
import xploreapp.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    val detail = uiState.groupDetail
    val isAdmin = detail?.createdById == uiState.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.name ?: stringResource(Res.string.group_detail_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin && detail != null) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.group_detail_change_visibility)) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.openChangeVisibilityDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.group_detail_delete_group), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.openDeleteConfirmation()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && detail == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null && detail == null) {
                Text(
                    text = uiState.errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (detail != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!detail.description.isNullOrBlank()) {
                        Text(
                            text = detail.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    var selectedTabIndex by remember { mutableStateOf(0) }
                    val tabs = listOf(
                        stringResource(Res.string.competition_tab_members) to detail.memberCount.toString(),
                        stringResource(Res.string.competition_tab_competitions) to uiState.competitions.size.toString()
                    )

                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, (title, subtitle) ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text("$title ($subtitle)") }
                            )
                        }
                    }

                    if (selectedTabIndex == 0) {
                        LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detail.members) { member ->
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = member.displayName,
                                        fontWeight = if (member.userId == uiState.currentUserId) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                trailingContent = {
                                    if (member.role == GroupRole.Admin) {
                                        Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                            Text(stringResource(Res.string.group_detail_admin))
                                        }
                                    }
                                }
                            )
                        }
                    }
                    } else {
                        // Competitions Tab
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (isAdmin) {
                                Button(
                                    onClick = viewModel::openCreateCompetitionDialog,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(Res.string.competition_btn_create))
                                }
                            }

                            if (uiState.competitions.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Text(stringResource(Res.string.competition_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                // Competitions List & Leaderboard
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(stringResource(Res.string.competition_tab_competitions), style = MaterialTheme.typography.titleMedium)
                                    }
                                    
                                    items(uiState.competitions) { comp ->
                                        val isSelected = comp.id == uiState.selectedCompetitionId
                                        Card(
                                            onClick = { viewModel.selectCompetition(comp.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(comp.name, fontWeight = FontWeight.Bold)
                                                if (!comp.startDate.isNullOrBlank()) {
                                                    Text("Inizio: ${comp.startDate}", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(stringResource(Res.string.competition_leaderboard_title), style = MaterialTheme.typography.titleMedium)
                                    }

                                    if (uiState.selectedCompetitionId == null) {
                                        item {
                                            Text(stringResource(Res.string.competition_no_leaderboard), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    } else {
                                        val leaderboard = uiState.activeCompetitionLeaderboard
                                        if (leaderboard.isNullOrEmpty()) {
                                            item {
                                                Text("La classifica è vuota.", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        } else {
                                            items(leaderboard) { entry ->
                                                ListItem(
                                                    colors = ListItemDefaults.colors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                    ),
                                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                                    leadingContent = {
                                                        Text(
                                                            text = "#${entry.rank}",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    },
                                                    headlineContent = {
                                                        Text(
                                                            text = entry.displayName ?: "Anonimo",
                                                            fontWeight = if (entry.userId == uiState.currentUserId) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    trailingContent = {
                                                        Text("${entry.points} pts", fontWeight = FontWeight.Bold)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    Button(
                        onClick = { viewModel.leaveGroup(onSuccess = onBack) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(Res.string.group_detail_exit_group))
                    }
                }
            }
        }
    }

    if (uiState.isCreateCompetitionDialogOpen) {
        CreateCompetitionDialog(
            onDismiss = viewModel::closeCreateCompetitionDialog,
            onSave = { name, type, start, end, rules ->
                viewModel.createCompetition(name, type, start, end, rules, onSuccess = {})
            }
        )
    }

    // Dialogs
    if (uiState.isChangeVisibilityDialogOpen && detail != null) {
        ChangeVisibilityDialog(
            initialAccessType = detail.accessType.value,
            onDismiss = viewModel::closeChangeVisibilityDialog,
            onSave = { accessType, password ->
                viewModel.changeVisibility(accessType, password)
            }
        )
    }

    if (uiState.isDeleteConfirmationOpen) {
        AlertDialog(
            onDismissRequest = viewModel::closeDeleteConfirmation,
            title = { Text(stringResource(Res.string.group_detail_delete_group)) },
            text = { Text(stringResource(Res.string.group_detail_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(onSuccess = onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.group_detail_btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeDeleteConfirmation) {
                    Text(stringResource(Res.string.group_detail_btn_cancel))
                }
            }
        )
    }
}

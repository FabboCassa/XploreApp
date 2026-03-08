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
    onNavigateToCreateCompetition: (String) -> Unit = {},
    onNavigateToCompetitionMap: (String, List<String>) -> Unit = { _, _ -> },
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
                        MembersTabContent(
                            members = detail.members,
                            currentUserId = uiState.currentUserId,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        CompetitionsTabContent(
                            competitions = uiState.competitions,
                            isAdmin = isAdmin,
                            selectedCompetitionId = uiState.selectedCompetitionId,
                            activeLeaderboard = uiState.activeCompetitionLeaderboard,
                            currentUserId = uiState.currentUserId,
                            onNavigateToCreateCompetition = { onNavigateToCreateCompetition(groupId) },
                            onSelectCompetition = viewModel::selectCompetition,
                            onNavigateToCompetitionMap = onNavigateToCompetitionMap,
                            modifier = Modifier.weight(1f)
                        )
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

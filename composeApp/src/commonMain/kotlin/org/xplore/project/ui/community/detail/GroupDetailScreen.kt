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
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.domain.model.GroupRole

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
                title = { Text(detail?.name ?: "Group Details") },
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
                                text = { Text("Change Visibility") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.openChangeVisibilityDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Group", color = MaterialTheme.colorScheme.error) },
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

                    Text(
                        text = "Members (${detail.memberCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

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
                                            Text("Admin")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.leaveGroup(onSuccess = onBack) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Exit Group")
                    }
                }
            }
        }
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
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete this group? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(onSuccess = onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeDeleteConfirmation) {
                    Text("Cancel")
                }
            }
        )
    }
}

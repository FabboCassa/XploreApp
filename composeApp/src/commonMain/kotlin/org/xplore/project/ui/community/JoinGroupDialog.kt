package org.xplore.project.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.domain.model.Group
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.community_join_group
import xploreapp.composeapp.generated.resources.community_search_groups
import xploreapp.composeapp.generated.resources.community_join
import xploreapp.composeapp.generated.resources.community_btn_cancel
import xploreapp.composeapp.generated.resources.community_members_count
import xploreapp.composeapp.generated.resources.community_password_required
import xploreapp.composeapp.generated.resources.community_password_label
import xploreapp.composeapp.generated.resources.community_no_groups

/**
 * Dialog for searching and joining groups. Shows live search results.
 * Password-protected groups show a lock icon.
 * InviteOnly groups are excluded by the backend.
 */
@Composable
fun JoinGroupDialog(
    searchQuery: String,
    searchResults: List<Group>,
    myGroupIds: Set<String>,
    onSearchQueryChanged: (String) -> Unit,
    onJoinGroup: (groupId: String, password: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.community_join_group),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = { Text(stringResource(Res.string.community_search_groups)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Results list
                if (searchResults.isEmpty() && searchQuery.length >= 2) {
                    Text(
                        text = stringResource(Res.string.community_no_groups),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val joinable = searchResults.filter { it.id !in myGroupIds }
                    items(joinable, key = { it.id }) { group ->
                        SearchGroupRow(
                            group = group,
                            onJoin = onJoinGroup,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.community_btn_cancel))
            }
        },
    )
}

/**
 * A single row in the search results.
 * Shows name, description, member count, and lock icon for password groups.
 * Tapping a locked group prompts for password.
 */
@Composable
private fun SearchGroupRow(
    group: Group,
    onJoin: (groupId: String, password: String?) -> Unit,
) {
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!group.description.isNullOrBlank()) {
                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }

                // Right: member count + lock icon + join button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.community_members_count, group.memberCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (group.isPasswordProtected) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(Res.string.community_password_required),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    TextButton(
                        onClick = {
                            if (group.isPasswordProtected) {
                                showPasswordPrompt = !showPasswordPrompt
                            } else {
                                onJoin(group.id, null)
                            }
                        },
                    ) {
                        Text(stringResource(Res.string.community_join))
                    }
                }
            }

            // Password input row (expanded when needed)
            if (showPasswordPrompt) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(stringResource(Res.string.community_password_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { onJoin(group.id, passwordInput) },
                        enabled = passwordInput.isNotBlank(),
                    ) {
                        Text(stringResource(Res.string.community_join))
                    }
                }
            }
        }
    }
}

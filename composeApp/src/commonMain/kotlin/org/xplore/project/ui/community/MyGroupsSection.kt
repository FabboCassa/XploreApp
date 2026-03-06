package org.xplore.project.ui.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.domain.model.Group
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.community_my_groups
import xploreapp.composeapp.generated.resources.community_no_groups
import xploreapp.composeapp.generated.resources.community_leave
import xploreapp.composeapp.generated.resources.community_members_count
import xploreapp.composeapp.generated.resources.community_create_tooltip
import xploreapp.composeapp.generated.resources.community_join_tooltip

private val GreenCreate = Color(0xFF4CAF50)
private val BlueJoin = Color(0xFF2196F3)

/**
 * Section showing the user's groups with "+" and "→" action buttons.
 */
@Composable
fun MyGroupsSection(
    groups: List<Group>,
    isGuest: Boolean,
    onCreateGroup: () -> Unit,
    onJoinGroup: () -> Unit,
    onLeaveGroup: (String) -> Unit,
    onGroupClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title row with action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.community_my_groups),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!isGuest) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // "+" Create button (green)
                        IconButton(
                            onClick = onCreateGroup,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = GreenCreate,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(Res.string.community_create_tooltip),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // "→" Join button (blue)
                        IconButton(
                            onClick = onJoinGroup,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = BlueJoin,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = stringResource(Res.string.community_join_tooltip),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Group list
            if (isGuest || groups.isEmpty()) {
                Text(
                    text = stringResource(Res.string.community_no_groups),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                groups.forEach { group ->
                    GroupCard(
                        group = group,
                        actionLabel = stringResource(Res.string.community_leave),
                        onAction = { onLeaveGroup(group.id) },
                        onClick = { onGroupClick(group.id) }
                    )
                }
            }
        }
    }
}

/**
 * Reusable card for a single group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCard(
    group: Group,
    actionLabel: String,
    onAction: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                        maxLines = 2,
                    )
                }
                Text(
                    text = stringResource(Res.string.community_members_count, group.memberCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

package org.xplore.project.ui.community.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.domain.model.Competition
import org.xplore.project.domain.model.CompetitionLeaderboardEntry
import org.xplore.project.domain.model.GroupMember
import org.xplore.project.domain.model.GroupRole
import xploreapp.composeapp.generated.resources.*

@Composable
fun MembersTabContent(
    members: List<GroupMember>,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(members) { member ->
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
                        fontWeight = if (member.userId == currentUserId) FontWeight.Bold else FontWeight.Normal
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
}

@Composable
fun CompetitionsTabContent(
    competitions: List<Competition>,
    isAdmin: Boolean,
    selectedCompetitionId: String?,
    activeLeaderboard: List<CompetitionLeaderboardEntry>?,
    currentUserId: String?,
    onNavigateToCreateCompetition: () -> Unit,
    onSelectCompetition: (String) -> Unit,
    onNavigateToCompetitionMap: (String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isAdmin) {
            Button(
                onClick = onNavigateToCreateCompetition,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.competition_btn_create))
            }
        }

        if (competitions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.competition_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(stringResource(Res.string.competition_tab_competitions), style = MaterialTheme.typography.titleMedium)
                }

                items(competitions) { comp ->
                    val isSelected = comp.id == selectedCompetitionId
                    Card(
                        onClick = { onSelectCompetition(comp.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(comp.name, fontWeight = FontWeight.Bold)
                            if (!comp.startDate.isNullOrBlank()) {
                                Text(stringResource(Res.string.competition_starts_at, comp.startDate ?: ""), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    val selectedComp = competitions.find { it.id == selectedCompetitionId }
                    if (selectedComp != null && (selectedComp.type.ordinal == 1 || selectedComp.type.ordinal == 2)) {
                        Button(
                            onClick = {
                                val allowedPois = selectedComp.rules.mapNotNull { it.targetPlaceId }
                                onNavigateToCompetitionMap(selectedComp.id, allowedPois)
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(stringResource(Res.string.competition_map_open))
                        }
                    }

                    Text(stringResource(Res.string.competition_leaderboard_title), style = MaterialTheme.typography.titleMedium)
                }

                if (selectedCompetitionId == null) {
                    item {
                        Text(stringResource(Res.string.competition_no_leaderboard), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    if (activeLeaderboard.isNullOrEmpty()) {
                        item {
                            Text(stringResource(Res.string.competition_leaderboard_empty), style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        items(activeLeaderboard.orEmpty()) { entry ->
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
                                        text = entry.displayName ?: stringResource(Res.string.competition_anonymous_user),
                                        fontWeight = if (entry.userId == currentUserId) FontWeight.Bold else FontWeight.Normal
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

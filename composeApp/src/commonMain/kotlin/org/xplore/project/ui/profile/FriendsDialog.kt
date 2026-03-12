package org.xplore.project.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.*

@Composable
fun FriendsDialog(
    friends: List<Friend>,
    sentRequests: List<FriendRequest>,
    receivedRequests: List<FriendRequest>,
    searchResults: List<SearchUser>,
    searchQuery: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onSendRequest: (String) -> Unit,
    onCancelRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.profile_friends_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text(stringResource(Res.string.profile_friends_tab_friends)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = { Text(stringResource(Res.string.profile_friends_tab_add)) },
                    )
                }

                Spacer(Modifier.height(4.dp))

                when (selectedTab) {
                    0 -> FriendsTab(
                        friends = friends,
                        receivedRequests = receivedRequests,
                        onAcceptRequest = onAcceptRequest,
                        onRejectRequest = onRejectRequest,
                        onRemove = onRemove,
                    )
                    1 -> AddFriendsTab(
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        sentRequests = sentRequests,
                        friends = friends,
                        onSearchChanged = onSearchChanged,
                        onSearch = onSearch,
                        onSendRequest = onSendRequest,
                        onCancelRequest = onCancelRequest,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

// ── Tab 0: My Friends ───────────────────────────────────────────
@Composable
private fun FriendsTab(
    friends: List<Friend>,
    receivedRequests: List<FriendRequest>,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.height(300.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Received requests section
        if (receivedRequests.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.profile_friends_received_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(receivedRequests, key = { it.id }) { request ->
                FriendRequestCard(
                    username = request.fromDisplayName ?: request.fromUserId,
                    trailingContent = {
                        IconButton(onClick = { onAcceptRequest(request.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = stringResource(Res.string.profile_friends_accept_request),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { onRejectRequest(request.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(Res.string.profile_friends_reject_request),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // Friends list
        if (friends.isEmpty() && receivedRequests.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.profile_friends_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }
        } else {
            items(friends, key = { it.id }) { friend ->
                FriendItemCard(
                    displayName = friend.displayName ?: friend.id,
                    trailing = {
                        IconButton(onClick = { onRemove(friend.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(Res.string.profile_friends_remove_btn),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )
            }
        }
    }
}

// ── Tab 1: Add Friends ──────────────────────────────────────────
@Composable
private fun AddFriendsTab(
    searchQuery: String,
    searchResults: List<SearchUser>,
    sentRequests: List<FriendRequest>,
    friends: List<Friend>,
    onSearchChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onSendRequest: (String) -> Unit,
    onCancelRequest: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Search field
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text(stringResource(Res.string.profile_friends_search_hint), maxLines = 1) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            )
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(Res.string.profile_friends_search_btn),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.height(240.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Search results — status comes from backend via SearchUser.friendshipStatus
            if (searchResults.isNotEmpty()) {
                items(searchResults, key = { "search_${it.userId}" }) { user ->
                    val alreadyFriend = user.friendshipStatus == "accepted"
                        || friends.any { it.id == user.userId }
                    val alreadySent = user.friendshipStatus == "pending"
                        || sentRequests.any { it.fromUserId == user.userId }

                    FriendItemCard(
                        displayName = user.displayName ?: user.userId,
                        trailing = {
                            when {
                                alreadyFriend -> {
                                    Text(
                                        text = stringResource(Res.string.profile_friends_already_friend),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                alreadySent -> {
                                    Text(
                                        text = stringResource(Res.string.profile_friends_request_sent),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                else -> {
                                    // Pass userId (not displayName) to the request
                                    IconButton(onClick = { onSendRequest(user.userId) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonAdd,
                                            contentDescription = stringResource(Res.string.profile_friends_send_request),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            } else if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = stringResource(Res.string.profile_friends_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }

            // Sent requests section
            if (sentRequests.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.profile_friends_sent_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(sentRequests, key = { "sent_${it.id}" }) { request ->
                    FriendRequestCard(
                        username = request.fromDisplayName ?: request.fromUserId,
                        trailingContent = {
                            IconButton(onClick = { onCancelRequest(request.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(Res.string.profile_friends_cancel_request),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

// ── Reusable cards ──────────────────────────────────────────────
@Composable
private fun FriendItemCard(
    displayName: String,
    trailing: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Mini avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
    }
}

@Composable
private fun FriendRequestCard(
    username: String,
    trailingContent: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = username.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                trailingContent()
            }
        }
    }
}

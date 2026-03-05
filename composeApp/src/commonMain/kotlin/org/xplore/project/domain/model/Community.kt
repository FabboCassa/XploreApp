package org.xplore.project.domain.model

/**
 * Access type for a community group.
 */
enum class GroupAccessType(val value: Int) {
    Public(0),
    Password(1),
    InviteOnly(2);

    companion object {
        fun fromValue(value: Int): GroupAccessType =
            entries.firstOrNull { it.value == value } ?: Public
    }
}

/**
 * Domain model for a community group.
 */
data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val createdById: String,
    val createdAt: String,
    val memberCount: Int,
    val accessType: GroupAccessType = GroupAccessType.Public,
    val isPasswordProtected: Boolean = false,
)

/**
 * Domain model for a leaderboard entry.
 */
data class LeaderboardEntry(
    val userId: String,
    val displayName: String?,
    val visitedPlacesCount: Int,
    val groupVictories: Int,
    val communityContributions: Int,
    val totalScore: Int,
    val rank: Int,
)

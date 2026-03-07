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

/**
 * Role of a user in a group.
 */
enum class GroupRole(val value: Int) {
    Member(0),
    Admin(1);

    companion object {
        fun fromValue(value: Int): GroupRole =
            entries.firstOrNull { it.value == value } ?: Member
    }
}

/**
 * Domain model for a user in a group.
 */
data class GroupMember(
    val userId: String,
    val displayName: String,
    val role: GroupRole,
    val joinedAt: String,
)

/**
 * Domain model for a group's full details.
 */
data class GroupDetail(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val createdById: String,
    val createdAt: String,
    val memberCount: Int,
    val accessType: GroupAccessType,
    val isPasswordProtected: Boolean,
    val members: List<GroupMember>,
)

/**
 * Types of competitions available within a group.
 */
enum class CompetitionType(val value: Int) {
    MostPlaces(0),
    FastestToPreset(1),
    ScavengerHunt(2);

    companion object {
        fun fromValue(value: Int): CompetitionType =
            entries.firstOrNull { it.value == value } ?: MostPlaces
    }
}

/**
 * Actions that award points in a competition.
 */
enum class CompetitionActionType(val value: Int) {
    VisitPlace(0),
    AnswerQuestion(1),
    FinishFirst(2);

    companion object {
        fun fromValue(value: Int): CompetitionActionType =
            entries.firstOrNull { it.value == value } ?: VisitPlace
    }
}

/**
 * Domain model for a competition rule.
 */
data class CompetitionRule(
    val id: String,
    val actionType: CompetitionActionType,
    val pointsAwarded: Int,
    val targetPlaceId: String? = null,
)

/**
 * Domain model for a competition.
 */
data class Competition(
    val id: String,
    val groupId: String,
    val name: String,
    val type: CompetitionType,
    val startDate: String?,
    val endDate: String?,
    val isActive: Boolean,
    val createdAt: String,
    val rules: List<CompetitionRule>
)

/**
 * Domain model for a single entry in a competition leaderboard.
 */
data class CompetitionLeaderboardEntry(
    val userId: String,
    val displayName: String?,
    val points: Int,
    val rank: Int,
)

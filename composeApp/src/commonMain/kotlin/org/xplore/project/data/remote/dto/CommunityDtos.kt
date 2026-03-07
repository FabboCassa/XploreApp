package org.xplore.project.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTO for a community group returned by the backend.
 */
@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val createdById: String,
    val createdAt: String,
    val memberCount: Int,
    val accessType: Int = 0,
    val isPasswordProtected: Boolean = false,
)

/**
 * Request body to create a new group.
 */
@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val accessType: Int = 0,
    val password: String? = null,
)

/**
 * Request body to join a password-protected group.
 */
@Serializable
data class JoinGroupRequest(
    val password: String? = null,
)

/**
 * DTO for a leaderboard entry returned by the backend.
 */
@Serializable
data class LeaderboardEntryDto(
    val userId: String,
    val displayName: String? = null,
    val visitedPlacesCount: Int,
    val groupVictories: Int,
    val communityContributions: Int,
    val totalScore: Int,
    val rank: Int,
)

/**
 * DTO for a single member of a group.
 */
@Serializable
data class GroupMemberDto(
    val userId: String,
    val displayName: String,
    val role: Int,
    val joinedAt: String,
)

/**
 * DTO for a group detail including members.
 */
@Serializable
data class GroupDetailDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val createdById: String,
    val createdAt: String,
    val memberCount: Int,
    val accessType: Int,
    val isPasswordProtected: Boolean,
    val members: List<GroupMemberDto>,
)

/**
 * Request body to change group visibility.
 */
@Serializable
data class ChangeGroupVisibilityRequest(
    val accessType: Int,
    val password: String? = null,
)

/**
 * Request body to record a visited place.
 */
@Serializable
data class VisitPlaceRequest(
    val placeId: String,
)

/**
 * Request body to create a new competition.
 */
@Serializable
data class CreateCompetitionRequest(
    val name: String,
    val type: Int,
    val startDate: String? = null,
    val endDate: String? = null,
    val rules: List<CompetitionRuleRequest>
)

/**
 * Request body to create a new competition rule.
 */
@Serializable
data class CompetitionRuleRequest(
    val actionType: Int,
    val pointsAwarded: Int = 1,
    val targetPlaceId: String? = null,
)

/**
 * DTO for a competition returned by the backend.
 */
@Serializable
data class CompetitionDto(
    val id: String,
    val groupId: String,
    val name: String,
    val type: Int,
    val startDate: String? = null,
    val endDate: String? = null,
    val isActive: Boolean,
    val createdAt: String,
    val rules: List<CompetitionRuleDto>
)

/**
 * DTO for a competition rule returned by the backend.
 */
@Serializable
data class CompetitionRuleDto(
    val id: String,
    val actionType: Int,
    val pointsAwarded: Int,
    val targetPlaceId: String? = null,
)

/**
 * DTO for a single entry in a competition leaderboard.
 */
@Serializable
data class CompetitionLeaderboardEntryDto(
    val userId: String,
    val displayName: String? = null,
    val points: Int,
    val rank: Int,
)

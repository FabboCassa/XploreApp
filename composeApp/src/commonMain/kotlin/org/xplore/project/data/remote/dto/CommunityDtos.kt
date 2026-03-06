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

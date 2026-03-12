package org.xplore.project.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTO for an accepted friend returned by the backend.
 */
@Serializable
data class FriendResponseDto(
    val userId: String,
    val displayName: String? = null,
    val hasAvatar: Boolean = false,
    val friendsSince: String,
)

/**
 * DTO for an incoming pending friend request returned by the backend.
 */
@Serializable
data class FriendRequestResponseDto(
    val requestId: String,
    val requesterId: String,
    val requesterDisplayName: String? = null,
    val requesterHasAvatar: Boolean = false,
    val sentAt: String,
)

/**
 * DTO for a user returned by the friend search endpoint.
 */
@Serializable
data class SearchUserResponseDto(
    val userId: String,
    val displayName: String? = null,
    val hasAvatar: Boolean = false,
    val friendshipStatus: String? = null,
)

/**
 * Request body to send a friend request.
 */
@Serializable
data class SendFriendRequestDto(
    val addresseeId: String,
)

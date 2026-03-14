package org.xplore.project.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTO for a group invite returned by the backend.
 */
@Serializable
data class GroupInviteResponseDto(
    val id: String,
    val groupId: String,
    val groupName: String,
    val invitedByUserId: String,
    val invitedByDisplayName: String? = null,
    val invitedUserId: String,
    val status: Int = 0,
    val createdAt: String,
)

/**
 * Request body to register a device token for push notifications.
 */
@Serializable
data class RegisterDeviceTokenRequest(
    val token: String,
    val platform: String,
)

/**
 * Request body to send a group invite.
 */
@Serializable
data class SendGroupInviteRequest(
    val invitedUserId: String,
)

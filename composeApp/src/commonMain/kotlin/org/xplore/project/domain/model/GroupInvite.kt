package org.xplore.project.domain.model

/**
 * Status of a group invite.
 */
enum class GroupInviteStatus(val value: Int) {
    Pending(0),
    Accepted(1),
    Rejected(2);

    companion object {
        fun fromValue(value: Int): GroupInviteStatus =
            entries.firstOrNull { it.value == value } ?: Pending
    }
}

/**
 * Domain model for a group invite.
 */
data class GroupInvite(
    val id: String,
    val groupId: String,
    val groupName: String,
    val invitedByUserId: String,
    val invitedByDisplayName: String?,
    val invitedUserId: String,
    val status: GroupInviteStatus,
    val createdAt: String,
)

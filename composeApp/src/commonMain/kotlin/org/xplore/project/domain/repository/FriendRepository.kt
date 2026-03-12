package org.xplore.project.domain.repository

import org.xplore.project.data.remote.dto.FriendRequestResponseDto
import org.xplore.project.data.remote.dto.FriendResponseDto
import org.xplore.project.data.remote.dto.SearchUserResponseDto

/**
 * Repository interface for friend request and friendship management.
 */
interface FriendRepository {
    suspend fun getFriends(): Result<List<FriendResponseDto>>
    suspend fun getPendingRequests(): Result<List<FriendRequestResponseDto>>
    suspend fun sendFriendRequest(addresseeId: String): Result<Unit>
    suspend fun acceptRequest(requestId: String): Result<Unit>
    suspend fun rejectRequest(requestId: String): Result<Unit>
    suspend fun removeFriend(friendUserId: String): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<SearchUserResponseDto>>
}

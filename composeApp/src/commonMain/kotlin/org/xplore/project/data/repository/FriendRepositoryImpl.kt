package org.xplore.project.data.repository

import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.remote.FriendApiService
import org.xplore.project.data.remote.dto.FriendRequestResponseDto
import org.xplore.project.data.remote.dto.FriendResponseDto
import org.xplore.project.data.remote.dto.SearchUserResponseDto
import org.xplore.project.domain.repository.FriendRepository

/**
 * Concrete implementation of [FriendRepository].
 */
class FriendRepositoryImpl(
    private val apiService: FriendApiService,
    private val tokenManager: TokenManager,
) : FriendRepository {

    override suspend fun getFriends(): Result<List<FriendResponseDto>> = runCatching {
        apiService.getFriends(requireToken())
    }

    override suspend fun getPendingRequests(): Result<List<FriendRequestResponseDto>> = runCatching {
        apiService.getPendingRequests(requireToken())
    }

    override suspend fun sendFriendRequest(addresseeId: String): Result<Unit> = runCatching {
        apiService.sendFriendRequest(requireToken(), addresseeId)
    }

    override suspend fun acceptRequest(requestId: String): Result<Unit> = runCatching {
        apiService.acceptRequest(requireToken(), requestId)
    }

    override suspend fun rejectRequest(requestId: String): Result<Unit> = runCatching {
        apiService.rejectRequest(requireToken(), requestId)
    }

    override suspend fun removeFriend(friendUserId: String): Result<Unit> = runCatching {
        apiService.removeFriend(requireToken(), friendUserId)
    }

    override suspend fun searchUsers(query: String): Result<List<SearchUserResponseDto>> = runCatching {
        apiService.searchUsers(requireToken(), query)
    }

    private fun requireToken(): String =
        tokenManager.accessToken ?: throw IllegalStateException("Not authenticated")
}

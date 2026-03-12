package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.xplore.project.data.remote.dto.FriendRequestResponseDto
import org.xplore.project.data.remote.dto.FriendResponseDto
import org.xplore.project.data.remote.dto.SearchUserResponseDto
import org.xplore.project.data.remote.dto.SendFriendRequestDto

/**
 * Remote data source for the Friends API endpoints.
 */
class FriendApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    /** Fetch accepted friends of the current user. */
    suspend fun getFriends(token: String): List<FriendResponseDto> {
        val response = httpClient.get("$baseUrl/api/friends") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Fetch incoming pending friend requests for the current user. */
    suspend fun getPendingRequests(token: String): List<FriendRequestResponseDto> {
        val response = httpClient.get("$baseUrl/api/friends/requests") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Send a friend request to another user by their ID. */
    suspend fun sendFriendRequest(token: String, addresseeId: String) {
        val response = httpClient.post("$baseUrl/api/friends/request") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SendFriendRequestDto(addresseeId))
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Accept a pending friend request by its ID. */
    suspend fun acceptRequest(token: String, requestId: String) {
        val response = httpClient.post("$baseUrl/api/friends/request/$requestId/accept") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Reject or cancel a friend request by its ID. */
    suspend fun rejectRequest(token: String, requestId: String) {
        val response = httpClient.post("$baseUrl/api/friends/request/$requestId/reject") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Remove an existing friend by their user ID. */
    suspend fun removeFriend(token: String, friendUserId: String) {
        val response = httpClient.delete("$baseUrl/api/friends/$friendUserId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Search users by display name or username. */
    suspend fun searchUsers(token: String, query: String): List<SearchUserResponseDto> {
        val response = httpClient.get("$baseUrl/api/friends/search") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("query", query)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }
}

package org.xplore.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.xplore.project.data.remote.dto.ChangeGroupVisibilityRequest
import org.xplore.project.data.remote.dto.CreateGroupRequest
import org.xplore.project.data.remote.dto.GroupDetailDto
import org.xplore.project.data.remote.dto.GroupDto
import org.xplore.project.data.remote.dto.JoinGroupRequest
import org.xplore.project.data.remote.dto.CompetitionDto
import org.xplore.project.data.remote.dto.CompetitionLeaderboardEntryDto
import org.xplore.project.data.remote.dto.CreateCompetitionRequest
import org.xplore.project.data.remote.dto.LeaderboardEntryDto
import org.xplore.project.data.remote.dto.VisitPlaceRequest

/**
 * Remote data source for the Community API endpoints.
 */
class CommunityRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    /** Fetch all available groups (paginated). InviteOnly excluded by backend. */
    suspend fun getAllGroups(page: Int = 1, pageSize: Int = 20): List<GroupDto> {
        val response = httpClient.get("$baseUrl/api/community/groups") {
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Search groups by name. InviteOnly excluded by backend. */
    suspend fun searchGroups(query: String): List<GroupDto> {
        val response = httpClient.get("$baseUrl/api/community/groups/search") {
            parameter("query", query)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Fetch groups the current user belongs to. */
    suspend fun getMyGroups(token: String): List<GroupDto> {
        val response = httpClient.get("$baseUrl/api/community/groups/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Create a new group. */
    suspend fun createGroup(request: CreateGroupRequest, token: String): GroupDto {
        val response = httpClient.post("$baseUrl/api/community/groups") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Join an existing group. Sends optional password. */
    suspend fun joinGroup(groupId: String, password: String?, token: String) {
        val response = httpClient.post("$baseUrl/api/community/groups/$groupId/join") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(JoinGroupRequest(password))
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Leave a group. */
    suspend fun leaveGroup(groupId: String, token: String) {
        val response = httpClient.post("$baseUrl/api/community/groups/$groupId/leave") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Fetch the global explorer leaderboard. Guests excluded by backend. */
    suspend fun getLeaderboard(top: Int = 50): List<LeaderboardEntryDto> {
        val response = httpClient.get("$baseUrl/api/community/leaderboard") {
            parameter("top", top)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Fetch complete details of a specific group by ID. */
    suspend fun getGroupDetail(groupId: String, token: String): GroupDetailDto {
        val response = httpClient.get("$baseUrl/api/community/groups/$groupId/detail") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Delete a group (Only Admin). */
    suspend fun deleteGroup(groupId: String, token: String) {
        val response = httpClient.delete("$baseUrl/api/community/groups/$groupId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Change group visibility (Only Admin). */
    suspend fun changeGroupVisibility(groupId: String, accessType: Int, password: String?, token: String) {
        val response = httpClient.put("$baseUrl/api/community/groups/$groupId/visibility") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ChangeGroupVisibilityRequest(accessType, password))
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Record a place as visited. */
    suspend fun visitPlace(placeId: String, token: String) {
        val response = httpClient.post("$baseUrl/api/community/visits") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(VisitPlaceRequest(placeId))
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Create a competition in a group. */
    suspend fun createCompetition(groupId: String, request: CreateCompetitionRequest, token: String): CompetitionDto {
        val response = httpClient.post("$baseUrl/api/community/groups/$groupId/competitions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Update an existing competition. */
    suspend fun updateCompetition(groupId: String, compId: String, request: CreateCompetitionRequest, token: String) {
        val response = httpClient.put("$baseUrl/api/community/groups/$groupId/competitions/$compId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
    }

    /** Fetch all competitions for a group. */
    suspend fun getCompetitions(groupId: String, token: String): List<CompetitionDto> {
        val response = httpClient.get("$baseUrl/api/community/groups/$groupId/competitions") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }

    /** Fetch leaderboard for a specific competition. */
    suspend fun getCompetitionLeaderboard(groupId: String, compId: String, token: String): List<CompetitionLeaderboardEntryDto> {
        val response = httpClient.get("$baseUrl/api/community/groups/$groupId/competitions/$compId/leaderboard") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("HTTP Error ${response.status.value}")
        }
        return response.body()
    }
}

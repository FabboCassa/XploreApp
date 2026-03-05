package org.xplore.project.data.repository

import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.remote.CommunityRemoteDataSource
import org.xplore.project.data.remote.dto.CreateGroupRequest
import org.xplore.project.data.remote.dto.GroupDto
import org.xplore.project.domain.model.Group
import org.xplore.project.domain.model.GroupAccessType
import org.xplore.project.domain.model.LeaderboardEntry
import org.xplore.project.domain.repository.CommunityRepository

/**
 * Concrete implementation of [CommunityRepository].
 */
class CommunityRepositoryImpl(
    private val remoteDataSource: CommunityRemoteDataSource,
    private val tokenManager: TokenManager,
) : CommunityRepository {

    override suspend fun getAllGroups(page: Int, pageSize: Int): List<Group> =
        remoteDataSource.getAllGroups(page, pageSize).map { it.toDomain() }

    override suspend fun searchGroups(query: String): List<Group> =
        remoteDataSource.searchGroups(query).map { it.toDomain() }

    override suspend fun getMyGroups(): List<Group> {
        val token = requireToken()
        return remoteDataSource.getMyGroups(token).map { it.toDomain() }
    }

    override suspend fun createGroup(
        name: String,
        description: String?,
        imageUrl: String?,
        accessType: Int,
        password: String?,
    ): Group {
        val token = requireToken()
        return remoteDataSource.createGroup(
            request = CreateGroupRequest(name, description, imageUrl, accessType, password),
            token = token,
        ).toDomain()
    }

    override suspend fun joinGroup(groupId: String, password: String?) {
        val token = requireToken()
        remoteDataSource.joinGroup(groupId, password, token)
    }

    override suspend fun leaveGroup(groupId: String) {
        val token = requireToken()
        remoteDataSource.leaveGroup(groupId, token)
    }

    override suspend fun getLeaderboard(top: Int): List<LeaderboardEntry> =
        remoteDataSource.getLeaderboard(top).map { dto ->
            LeaderboardEntry(
                userId = dto.userId,
                displayName = dto.displayName,
                visitedPlacesCount = dto.visitedPlacesCount,
                groupVictories = dto.groupVictories,
                communityContributions = dto.communityContributions,
                totalScore = dto.totalScore,
                rank = dto.rank,
            )
        }

    private fun requireToken(): String =
        tokenManager.accessToken ?: throw IllegalStateException("Not authenticated")

    private fun GroupDto.toDomain() = Group(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        createdById = createdById,
        createdAt = createdAt,
        memberCount = memberCount,
        accessType = GroupAccessType.fromValue(accessType),
        isPasswordProtected = isPasswordProtected,
    )
}

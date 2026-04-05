package org.xplore.project.data.repository

import org.xplore.project.data.local.PendingVisitLocalDataSource
import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.remote.CommunityRemoteDataSource
import org.xplore.project.data.remote.dto.CreateGroupRequest
import org.xplore.project.data.remote.dto.GroupDetailDto
import org.xplore.project.data.remote.dto.GroupDto
import org.xplore.project.domain.model.Group
import org.xplore.project.domain.model.GroupAccessType
import org.xplore.project.domain.model.GroupDetail
import org.xplore.project.domain.model.GroupMember
import org.xplore.project.domain.model.GroupRole
import org.xplore.project.domain.model.LeaderboardEntry
import org.xplore.project.domain.model.Competition
import org.xplore.project.domain.model.CompetitionType
import org.xplore.project.domain.model.CompetitionRule
import org.xplore.project.domain.model.CompetitionActionType
import org.xplore.project.domain.model.CompetitionLeaderboardEntry
import org.xplore.project.domain.model.GroupInvite
import org.xplore.project.domain.model.GroupInviteStatus
import org.xplore.project.data.remote.dto.CompetitionDto
import org.xplore.project.data.remote.dto.CompetitionRuleDto
import org.xplore.project.data.remote.dto.CompetitionLeaderboardEntryDto
import org.xplore.project.data.remote.dto.CreateCompetitionRequest
import org.xplore.project.data.remote.dto.CompetitionRuleRequest
import org.xplore.project.data.remote.dto.GroupInviteResponseDto
import org.xplore.project.domain.repository.CommunityRepository

/**
 * Concrete implementation of [CommunityRepository].
 */
class CommunityRepositoryImpl(
    private val remoteDataSource: CommunityRemoteDataSource,
    private val tokenManager: TokenManager,
    private val pendingVisitLocalDataSource: PendingVisitLocalDataSource,
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

    override suspend fun getGroupDetail(groupId: String): GroupDetail {
        val token = requireToken()
        return remoteDataSource.getGroupDetail(groupId, token).toDomain()
    }

    override suspend fun deleteGroup(groupId: String) {
        val token = requireToken()
        remoteDataSource.deleteGroup(groupId, token)
    }

    override suspend fun changeGroupVisibility(groupId: String, accessType: Int, password: String?) {
        val token = requireToken()
        remoteDataSource.changeGroupVisibility(groupId, accessType, password, token)
    }

    override suspend fun visitPlace(placeId: String) {
        val token = requireToken()
        try {
            remoteDataSource.visitPlace(placeId, token)
        } catch (e: Exception) {
            // Server unreachable — queue visit for later sync
            println("🔌 [Community] visitPlace failed: ${e.message} — queuing for offline sync")
            pendingVisitLocalDataSource.insertVisit(placeId)
        }
    }

    override suspend fun syncPendingVisits() {
        val token = tokenManager.accessToken ?: return
        // Don't try to sync if we're using offline guest tokens
        if (token == "offline_guest") return

        val pending = pendingVisitLocalDataSource.getAllPending()
        if (pending.isEmpty()) return

        println("🔄 [Community] Syncing ${pending.size} pending visits...")
        for (placeId in pending) {
            try {
                remoteDataSource.visitPlace(placeId, token)
                pendingVisitLocalDataSource.deleteVisit(placeId)
                println("🔄 [Community] ✅ Synced visit for $placeId")
            } catch (e: Exception) {
                // Still offline — stop trying, will retry next time
                println("🔄 [Community] ❌ Sync failed for $placeId: ${e.message} — will retry later")
                break
            }
        }
    }

    override suspend fun createCompetition(
        groupId: String,
        name: String,
        type: Int,
        startDate: String?,
        endDate: String?,
        rules: List<CompetitionRuleRequest>
    ): Competition {
        val token = requireToken()
        return remoteDataSource.createCompetition(
            groupId,
            CreateCompetitionRequest(name, type, startDate, endDate, rules),
            token
        ).toDomain()
    }

    override suspend fun updateCompetition(
        groupId: String,
        compId: String,
        name: String,
        type: Int,
        startDate: String?,
        endDate: String?,
        rules: List<CompetitionRuleRequest>
    ) {
        val token = requireToken()
        remoteDataSource.updateCompetition(
            groupId,
            compId,
            CreateCompetitionRequest(name, type, startDate, endDate, rules),
            token
        )
    }

    override suspend fun getCompetitions(groupId: String): List<Competition> {
        val token = requireToken()
        return remoteDataSource.getCompetitions(groupId, token).map { it.toDomain() }
    }

    override suspend fun getCompetitionLeaderboard(
        groupId: String,
        compId: String
    ): List<CompetitionLeaderboardEntry> {
        val token = requireToken()
        return remoteDataSource.getCompetitionLeaderboard(groupId, compId, token).map { it.toDomain() }
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

    private fun GroupDetailDto.toDomain() = GroupDetail(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        createdById = createdById,
        createdAt = createdAt,
        memberCount = memberCount,
        accessType = GroupAccessType.fromValue(accessType),
        isPasswordProtected = isPasswordProtected,
        members = members.map { m ->
            GroupMember(
                userId = m.userId,
                displayName = m.displayName,
                role = GroupRole.fromValue(m.role),
                joinedAt = m.joinedAt
            )
        }
    )

    private fun CompetitionRuleDto.toDomain() = CompetitionRule(
        id = id,
        actionType = CompetitionActionType.fromValue(actionType),
        pointsAwarded = pointsAwarded,
        targetPlaceId = targetPlaceId,
    )

    private fun CompetitionDto.toDomain() = Competition(
        id = id,
        groupId = groupId,
        name = name,
        type = CompetitionType.fromValue(type),
        startDate = startDate,
        endDate = endDate,
        isActive = isActive,
        createdAt = createdAt,
        rules = rules.map { it.toDomain() }
    )

    private fun CompetitionLeaderboardEntryDto.toDomain() = CompetitionLeaderboardEntry(
        userId = userId,
        displayName = displayName,
        points = points,
        rank = rank,
    )

    override suspend fun getMyGroupInvites(): List<GroupInvite> {
        val token = requireToken()
        return remoteDataSource.getMyGroupInvites(token).map { it.toDomain() }
    }

    override suspend fun sendGroupInvite(groupId: String, invitedUserId: String) {
        val token = requireToken()
        remoteDataSource.sendGroupInvite(groupId, invitedUserId, token)
    }

    override suspend fun acceptGroupInvite(inviteId: String) {
        val token = requireToken()
        remoteDataSource.acceptGroupInvite(inviteId, token)
    }

    override suspend fun rejectGroupInvite(inviteId: String) {
        val token = requireToken()
        remoteDataSource.rejectGroupInvite(inviteId, token)
    }

    private fun GroupInviteResponseDto.toDomain() = GroupInvite(
        id = id,
        groupId = groupId,
        groupName = groupName,
        invitedByUserId = invitedByUserId,
        invitedByDisplayName = invitedByDisplayName,
        invitedUserId = invitedUserId,
        status = GroupInviteStatus.fromValue(status),
        createdAt = createdAt,
    )
}

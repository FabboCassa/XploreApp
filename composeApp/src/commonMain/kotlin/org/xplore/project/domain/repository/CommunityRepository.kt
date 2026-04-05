package org.xplore.project.domain.repository

import org.xplore.project.domain.model.Group
import org.xplore.project.domain.model.GroupInvite
import org.xplore.project.domain.model.LeaderboardEntry

/**
 * Repository interface for community features.
 */
interface CommunityRepository {
    suspend fun getAllGroups(page: Int = 1, pageSize: Int = 20): List<Group>
    suspend fun searchGroups(query: String): List<Group>
    suspend fun getMyGroups(): List<Group>
    suspend fun createGroup(
        name: String,
        description: String?,
        imageUrl: String?,
        accessType: Int,
        password: String?,
    ): Group
    suspend fun joinGroup(groupId: String, password: String? = null)
    suspend fun leaveGroup(groupId: String)
    suspend fun getGroupDetail(groupId: String): org.xplore.project.domain.model.GroupDetail
    suspend fun deleteGroup(groupId: String)
    suspend fun changeGroupVisibility(groupId: String, accessType: Int, password: String?)
    suspend fun getLeaderboard(top: Int = 50): List<LeaderboardEntry>
    
    // Competitions & Visits
    suspend fun visitPlace(placeId: String)
    /** Sync any visits that were recorded while offline to the backend. */
    suspend fun syncPendingVisits()
    suspend fun createCompetition(
        groupId: String,
        name: String,
        type: Int,
        startDate: String?,
        endDate: String?,
        rules: List<org.xplore.project.data.remote.dto.CompetitionRuleRequest>
    ): org.xplore.project.domain.model.Competition
    suspend fun updateCompetition(
        groupId: String,
        compId: String,
        name: String,
        type: Int,
        startDate: String?,
        endDate: String?,
        rules: List<org.xplore.project.data.remote.dto.CompetitionRuleRequest>
    )
    suspend fun getCompetitions(groupId: String): List<org.xplore.project.domain.model.Competition>
    suspend fun getCompetitionLeaderboard(groupId: String, compId: String): List<org.xplore.project.domain.model.CompetitionLeaderboardEntry>

    // Group Invites
    suspend fun getMyGroupInvites(): List<GroupInvite>
    suspend fun sendGroupInvite(groupId: String, invitedUserId: String)
    suspend fun acceptGroupInvite(inviteId: String)
    suspend fun rejectGroupInvite(inviteId: String)
}

package org.xplore.project.domain.repository

import org.xplore.project.domain.model.Group
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
    suspend fun getLeaderboard(top: Int = 50): List<LeaderboardEntry>
}

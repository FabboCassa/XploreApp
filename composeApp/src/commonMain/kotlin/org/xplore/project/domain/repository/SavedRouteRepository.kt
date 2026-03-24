package org.xplore.project.domain.repository

import org.xplore.project.domain.model.SavedRoute

/**
 * Repository interface for saved routes.
 */
interface SavedRouteRepository {
    suspend fun getSavedRoutes(): Result<List<SavedRoute>>
    suspend fun getCompletedRoutes(): Result<List<SavedRoute>>
    suspend fun createRoute(
        name: String,
        description: String?,
        waypoints: List<org.xplore.project.data.remote.dto.SavedRouteWaypointRequest>,
    ): Result<SavedRoute>
    suspend fun markRouteCompleted(routeId: String): Result<Unit>
    suspend fun deleteRoute(routeId: String): Result<Unit>
    suspend fun getSharedRoute(shareToken: String): Result<SavedRoute>
}

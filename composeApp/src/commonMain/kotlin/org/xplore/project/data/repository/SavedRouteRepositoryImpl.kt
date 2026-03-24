package org.xplore.project.data.repository

import org.xplore.project.data.remote.RoutesRemoteDataSource
import org.xplore.project.data.remote.dto.CreateSavedRouteRequest
import org.xplore.project.data.remote.dto.SavedRouteDto
import org.xplore.project.data.remote.dto.SavedRouteWaypointRequest
import org.xplore.project.domain.model.SavedRoute
import org.xplore.project.domain.model.SavedRouteWaypoint
import org.xplore.project.domain.repository.SavedRouteRepository

class SavedRouteRepositoryImpl(
    private val remoteDataSource: RoutesRemoteDataSource,
) : SavedRouteRepository {

    override suspend fun getSavedRoutes(): Result<List<SavedRoute>> = runCatching {
        remoteDataSource.getSavedRoutes().map { it.toDomain() }
    }

    override suspend fun getCompletedRoutes(): Result<List<SavedRoute>> = runCatching {
        remoteDataSource.getCompletedRoutes().map { it.toDomain() }
    }

    override suspend fun createRoute(
        name: String,
        description: String?,
        waypoints: List<SavedRouteWaypointRequest>,
    ): Result<SavedRoute> = runCatching {
        val request = CreateSavedRouteRequest(
            name = name,
            description = description,
            waypoints = waypoints,
        )
        remoteDataSource.createRoute(request).toDomain()
    }

    override suspend fun markRouteCompleted(routeId: String): Result<Unit> = runCatching {
        remoteDataSource.markRouteCompleted(routeId)
    }

    override suspend fun deleteRoute(routeId: String): Result<Unit> = runCatching {
        remoteDataSource.deleteRoute(routeId)
    }

    override suspend fun getSharedRoute(shareToken: String): Result<SavedRoute> = runCatching {
        remoteDataSource.getSharedRoute(shareToken).toDomain()
    }

    private fun SavedRouteDto.toDomain() = SavedRoute(
        id = id,
        userId = userId,
        name = name,
        description = description,
        isCompleted = isCompleted,
        shareToken = shareToken,
        createdAt = createdAt,
        waypoints = waypoints.map { w ->
            SavedRouteWaypoint(
                id = w.id,
                placeId = w.placeId,
                name = w.name,
                latitude = w.latitude,
                longitude = w.longitude,
                orderIndex = w.orderIndex,
            )
        },
    )
}

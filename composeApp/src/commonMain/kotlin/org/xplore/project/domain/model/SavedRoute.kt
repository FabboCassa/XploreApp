package org.xplore.project.domain.model

/**
 * Represents a saved route created by a user.
 */
data class SavedRoute(
    val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val isCompleted: Boolean,
    val shareToken: String,
    val createdAt: String,
    val waypoints: List<SavedRouteWaypoint>,
)

/**
 * Represents a single waypoint/stop within a saved route.
 */
data class SavedRouteWaypoint(
    val id: String,
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val orderIndex: Int,
)

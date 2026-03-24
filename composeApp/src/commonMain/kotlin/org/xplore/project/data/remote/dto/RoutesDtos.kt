package org.xplore.project.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTO for a saved route returned by the backend.
 */
@Serializable
data class SavedRouteDto(
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val isCompleted: Boolean,
    val shareToken: String,
    val createdAt: String,
    val waypoints: List<SavedRouteWaypointDto> = emptyList(),
)

/**
 * DTO for a single waypoint in a route.
 */
@Serializable
data class SavedRouteWaypointDto(
    val id: String,
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val orderIndex: Int,
)

/**
 * Request body to create a new saved route.
 */
@Serializable
data class CreateSavedRouteRequest(
    val name: String,
    val description: String? = null,
    val waypoints: List<SavedRouteWaypointRequest>,
)

/**
 * Request body for a single waypoint when creating a route.
 */
@Serializable
data class SavedRouteWaypointRequest(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val orderIndex: Int,
)

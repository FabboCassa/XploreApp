package org.xplore.project.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain entity representing a Museum or Cultural Site.
 *
 * ## Domain Layer
 * This class is a strict **Domain Model**. It has no dependencies on API DTOs or Database entities.
 * It strictly represents the business view of a Museum.
 *
 * @property id Unique string identifier (UUID).
 * @property name The display name of the museum.
 * @property description A brief summary or translated description.
 * @property latitude Geographic latitude.
 * @property longitude Geographic longitude.
 * @property imageUrl Optional URL for the cover image.
 * @property address Physical address.
 * @property rating Average visitor rating (0.0 - 5.0).
 */
@Serializable
data class Museum(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val address: String? = null,
    val rating: Double? = null,
)

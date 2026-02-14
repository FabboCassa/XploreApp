package org.xplore.project.domain.model

/**
 * A pin on the map representing a cultural point of interest.
 * This is the data model consumed by [XploreMap] to render markers.
 */
data class MapPin(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val type: PinType,
)

/**
 * Category of a map pin — determines the icon and filtering behavior.
 */
enum class PinType {
    MUSEUM,
    ARTWORK,
    EVENT,
}

package org.xplore.project.domain.model

/**
 * A pin on the map representing a point of interest.
 * This is the data model consumed by [XploreMap] to render markers.
 */
data class MapPin(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val type: PinType,
    val description: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val openingHours: String? = null,
    val fee: String? = null,
    val phone: String? = null,
    val website: String? = null,
)

/**
 * Broad classification of a map pin.
 */
enum class PinType {
    MUSEUM,
    ARTWORK,
    HISTORIC,
    RELIGIOUS,
    NATURE,
    CULTURE,
    ATTRACTION,
    VIEWPOINT,
    EVENT,
    OTHER;

    companion object {
        fun fromString(value: String): PinType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
    }
}

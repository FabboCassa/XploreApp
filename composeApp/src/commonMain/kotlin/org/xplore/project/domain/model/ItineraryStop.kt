package org.xplore.project.domain.model

/**
 * Represents a single stop within an itinerary.
 *
 * @param pin The underlying map pin (POI) data.
 */
data class ItineraryStop(
    val pin: MapPin,
)

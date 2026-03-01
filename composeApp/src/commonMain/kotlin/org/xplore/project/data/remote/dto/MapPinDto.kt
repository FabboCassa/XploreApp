package org.xplore.project.data.remote.dto

import kotlinx.serialization.Serializable
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.PinType

/**
 * Network DTO for the POI proxy response.
 * Maps to the backend's `MapPinResponse` record.
 */
@Serializable
data class MapPinDto(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val description: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val openingHours: String? = null,
    val fee: String? = null,
    val phone: String? = null,
    val website: String? = null,
) {
    fun toDomain(): MapPin = MapPin(
        id = id,
        label = label,
        latitude = latitude,
        longitude = longitude,
        type = PinType.fromString(type),
        description = description,
        category = category,
        imageUrl = imageUrl,
        openingHours = openingHours,
        fee = fee,
        phone = phone,
        website = website,
    )
}

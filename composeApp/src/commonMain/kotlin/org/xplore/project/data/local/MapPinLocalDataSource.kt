package org.xplore.project.data.local

import io.ktor.util.date.getTimeMillis
import org.xplore.project.data.local.db.XploreDatabase
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.PinType
import kotlin.time.Duration.Companion.days

/**
 * Local cache backed by SQLDelight.
 * Stores POIs for offline access with 30-day auto-expiry.
 */
class MapPinLocalDataSource(private val db: XploreDatabase) {

    private val queries get() = db.mapPinCacheQueries
    private val expiryDuration = 30.days

    fun getCachedPins(): List<MapPin> =
        queries.selectAll().executeAsList().map { it.toDomain() }

    fun getCachedPinsInArea(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
    ): List<MapPin> =
        queries.selectInBoundingBox(minLat, maxLat, minLon, maxLon)
            .executeAsList()
            .map { it.toDomain() }

    private fun org.xplore.project.data.local.db.MapPinCache.toDomain() = MapPin(
        id = id,
        label = label,
        latitude = latitude,
        longitude = longitude,
        type = PinType.fromString(type),
        description = description,
        category = category,
        imageUrl = imageUrl,
    )

    fun cachePins(pins: List<MapPin>) {
        val now = getTimeMillis()
        pins.forEach { pin ->
            queries.insertPin(
                id = pin.id,
                label = pin.label,
                latitude = pin.latitude,
                longitude = pin.longitude,
                type = pin.type.name,
                description = pin.description,
                category = pin.category,
                imageUrl = pin.imageUrl,
                cachedAt = now,
            )
        }
    }

    fun clearExpiredCache() {
        val now = getTimeMillis()
        val cutoff = now - expiryDuration.inWholeMilliseconds
        queries.deleteExpired(cutoff)
    }

    fun clearAllCache() {
        queries.deleteAll()
    }

    /**
     * Deletes cached POIs that are OUTSIDE the given bounding box.
     * Used for radius-decrease pruning after 30 minutes.
     */
    fun deleteOutsideBoundingBox(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
    ) {
        queries.deleteOutsideBoundingBox(minLat, maxLat, minLon, maxLon)
    }

    /**
     * Searches cached POIs whose label contains the given query (case-insensitive).
     */
    fun searchByName(query: String): List<MapPin> =
        queries.searchByName(query).executeAsList().map { it.toDomain() }
}

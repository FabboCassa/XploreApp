package org.xplore.project.data.local

import kotlinx.datetime.Clock
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
        queries.selectAll().executeAsList().map { row ->
            MapPin(
                id = row.id,
                label = row.label,
                latitude = row.latitude,
                longitude = row.longitude,
                type = PinType.fromString(row.type),
                description = row.description,
                category = row.category,
                imageUrl = row.imageUrl,
            )
        }

    fun cachePins(pins: List<MapPin>) {
        val now = Clock.System.now().toEpochMilliseconds()
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
        val cutoff = Clock.System.now().minus(expiryDuration).toEpochMilliseconds()
        queries.deleteExpired(cutoff)
    }

    fun clearAllCache() {
        queries.deleteAll()
    }
}

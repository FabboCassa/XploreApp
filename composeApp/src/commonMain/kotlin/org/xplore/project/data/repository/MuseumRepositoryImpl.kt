package org.xplore.project.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.xplore.project.data.local.MapPinLocalDataSource
import org.xplore.project.data.remote.MapPinRemoteDataSource
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.Museum
import org.xplore.project.domain.repository.MuseumRepository
import kotlin.math.PI
import kotlin.math.cos

/**
 * Concrete implementation of the [MuseumRepository].
 *
 * ## Data Strategy — Cache-First + Background Refresh
 * 1. Check the local SQLDelight cache for POIs within the requested area.
 * 2. If cached data exists → return it **immediately** (instant UI).
 * 3. Fire a background network request to refresh the cache for next time.
 * 4. If the cache is empty → block on the network call and cache the result.
 * 5. Auto-expiry: cleans entries older than 30 days once per session.
 */
class MuseumRepositoryImpl(
    private val localDataSource: MapPinLocalDataSource,
    private val remoteDataSource: MapPinRemoteDataSource,
) : MuseumRepository {

    private var hasCleanedExpired = false

    /** Background scope for silent refresh (survives the calling coroutine). */
    private val refreshScope = CoroutineScope(Dispatchers.IO)

    override suspend fun getMuseums(): List<Museum> {
        return emptyList()
    }

    override suspend fun getMapPins(
        lat: Double,
        lon: Double,
        radiusKm: Double,
        allowNetworkRefresh: Boolean
    ): List<MapPin> {
        // Clean expired cache once per session
        if (!hasCleanedExpired) {
            try {
                println("📱 [Repository] Clearing expired cache (>30 days)...")
                localDataSource.clearExpiredCache()
            } catch (e: Exception) {
                println("📱 [Repository] ⚠️ Cache cleanup failed (schema change?): ${e.message}")
            }
            hasCleanedExpired = true
        }

        println("📱 [Repository] getMapPins: lat=$lat, lon=$lon, radius=${radiusKm}km")

        // ── 1. Compute bounding box ───────────────────────────────
        val (minLat, maxLat, minLon, maxLon) = boundingBox(lat, lon, radiusKm)

        // ── 2. Try the local cache first ──────────────────────────
        val cached = try {
            localDataSource.getCachedPinsInArea(minLat, maxLat, minLon, maxLon)
        } catch (e: Exception) {
            println("📱 [Repository] ⚠️ Cache read failed: ${e.message}")
            emptyList()
        }

        if (cached.isNotEmpty()) {
            println("📱 [Repository] ✅ Cache hit — ${cached.size} POIs returned instantly")

            if (allowNetworkRefresh) {
                // Silently refresh in background so data stays fresh
                refreshScope.launch {
                    try {
                        println("📱 [Repository] 🔄 Background refresh started...")
                        val fresh = remoteDataSource.fetchPins(lat, lon, radiusKm)
                            .map { it.toDomain() }
                        localDataSource.cachePins(fresh)
                        println("📱 [Repository] 🔄 Background refresh done — ${fresh.size} POIs updated")
                    } catch (e: Exception) {
                        println("📱 [Repository] 🔄 Background refresh failed (non-blocking): ${e.message}")
                    }
                }
            }

            return cached
        }

        // ── 3. Cache empty → fetch from network (blocking) ────────
        println("📱 [Repository] 📡 Cache empty — fetching from network...")

        return try {
            val remotePins = remoteDataSource.fetchPins(lat, lon, radiusKm)
                .map { it.toDomain() }

            println("📱 [Repository] ← Received ${remotePins.size} POIs from backend")
            remotePins.take(3).forEach { pin ->
                println("📱   POI: ${pin.id} | ${pin.label} | ${pin.category} | ${pin.type}")
            }
            if (remotePins.size > 3) println("📱   ... and ${remotePins.size - 3} more")

            // Cache for next time
            try {
                localDataSource.cachePins(remotePins)
                println("📱 [Repository] ✅ Cached ${remotePins.size} POIs to SQLDelight")
            } catch (e: Exception) {
                println("📱 [Repository] ⚠️ Caching failed: ${e.message}")
            }

            remotePins
        } catch (e: Exception) {
            println("📱 [Repository] ❌ Network also failed: ${e.message}")
            emptyList()
        }
    }

    // ── Bounding-box helper ──────────────────────────────────────
    /** Returns (minLat, maxLat, minLon, maxLon) for the given centre & radius. */
    private fun boundingBox(lat: Double, lon: Double, radiusKm: Double): BBox {
        val latDelta = radiusKm / 111.0                         // ~111 km per degree lat
        val lonDelta = radiusKm / (111.0 * cos(lat * PI / 180.0))  // adjust for longitude
        return BBox(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLon = lon - lonDelta,
            maxLon = lon + lonDelta,
        )
    }

    private data class BBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
    )

    override suspend fun clearMapCache() {
        println("📱 [Repository] 🗑️ Clearing ALL map cache...")
        try {
            localDataSource.clearAllCache()
            println("📱 [Repository] ✅ Map cache cleared")
        } catch (e: Exception) {
            println("📱 [Repository] ⚠️ Cache clear failed: ${e.message}")
        }
    }

    override suspend fun pruneCacheOutsideRadius(lat: Double, lon: Double, radiusKm: Double) {
        println("📱 [Repository] ✂️ Pruning cache outside ${radiusKm}km...")
        try {
            val (minLat, maxLat, minLon, maxLon) = boundingBox(lat, lon, radiusKm)
            localDataSource.deleteOutsideBoundingBox(minLat, maxLat, minLon, maxLon)
            println("📱 [Repository] ✅ Pruned cache to ${radiusKm}km bounding box")
        } catch (e: Exception) {
            println("📱 [Repository] ⚠️ Pruning failed: ${e.message}")
        }
    }
}

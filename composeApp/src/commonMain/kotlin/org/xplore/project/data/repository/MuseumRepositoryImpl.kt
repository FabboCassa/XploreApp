package org.xplore.project.data.repository

import org.xplore.project.data.local.MapPinLocalDataSource
import org.xplore.project.data.remote.MapPinRemoteDataSource
import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.Museum
import org.xplore.project.domain.repository.MuseumRepository

/**
 * Concrete implementation of the [MuseumRepository].
 *
 * ## Data Strategy (Stateless Proxy + Client Cache)
 * - **Network first**: Calls the backend proxy (which queries OpenStreetMap).
 * - **Cache locally**: Stores fetched POIs in SQLDelight for offline use.
 * - **Offline fallback**: If the network call fails, returns cached data.
 * - **Auto-expiry**: Cleans entries older than 30 days on first use.
 */
class MuseumRepositoryImpl(
    private val localDataSource: MapPinLocalDataSource,
    private val remoteDataSource: MapPinRemoteDataSource,
) : MuseumRepository {

    private var hasCleanedExpired = false

    override suspend fun getMuseums(): List<Museum> {
        return emptyList()
    }

    override suspend fun getMapPins(lat: Double, lon: Double, radiusKm: Double): List<MapPin> {
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

        println("📱 [Repository] Fetching POIs: lat=$lat, lon=$lon, radius=${radiusKm}km")

        return try {
            // 1. Try fetching from the backend proxy (network)
            println("📱 [Repository] → Calling backend /api/map/pois ...")
            val remotePins = remoteDataSource.fetchPins(lat, lon, radiusKm)
                .map { it.toDomain() }

            println("📱 [Repository] ← Received ${remotePins.size} POIs from backend")
            remotePins.take(3).forEach { pin ->
                println("📱   POI: ${pin.id} | ${pin.label} | ${pin.category} | ${pin.type}")
            }
            if (remotePins.size > 3) println("📱   ... and ${remotePins.size - 3} more")

            // 2. Try to cache — don't let cache failure discard remote data
            try {
                localDataSource.cachePins(remotePins)
                println("📱 [Repository] ✅ Cached ${remotePins.size} POIs to SQLDelight")
            } catch (e: Exception) {
                println("📱 [Repository] ⚠️ Caching failed (schema migration?): ${e.message}")
                // Remote data is still valid — continue without caching
            }

            remotePins
        } catch (e: Exception) {
            // 3. Network failed → fall back to local cache
            println("📱 [Repository] ❌ Network failed: ${e.message}")
            try {
                val cached = localDataSource.getCachedPins()
                println("📱 [Repository] 📦 Falling back to ${cached.size} cached POIs")
                cached
            } catch (cacheError: Exception) {
                println("📱 [Repository] ⚠️ Cache read also failed: ${cacheError.message}")
                emptyList()
            }
        }
    }

    override suspend fun clearMapCache() {
        println("📱 [Repository] 🗑️ Clearing ALL map cache...")
        try {
            localDataSource.clearAllCache()
            println("📱 [Repository] ✅ Map cache cleared")
        } catch (e: Exception) {
            println("📱 [Repository] ⚠️ Cache clear failed: ${e.message}")
        }
    }
}

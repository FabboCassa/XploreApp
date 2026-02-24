package org.xplore.project.domain.repository

import org.xplore.project.domain.model.MapPin
import org.xplore.project.domain.model.Museum

/**
 * Repository contract for fetching and managing Museum data.
 *
 * ## Clean Architecture
 * This interface lives in the **Domain Layer**. It adheres to the Dependency Inversion Principle:
 * - The **Domain** defines *what* data it needs.
 * - The **Data Layer** implements this interface (e.g., `MuseumRepositoryImpl`).
 *
 * This allows the Domain to remain independent of specific data sources (Network/DB).
 */
interface MuseumRepository {
    suspend fun getMuseums(): List<Museum>

    /**
     * Fetches POIs for the given map viewport.
     * Tries network first, falls back to local cache if offline.
     */
    suspend fun getMapPins(lat: Double, lon: Double, radiusKm: Double): List<MapPin>

    /**
     * Clears all locally cached map data.
     * Does NOT affect user login, progress, or saved places.
     */
    suspend fun clearMapCache()
}

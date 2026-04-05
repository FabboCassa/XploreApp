package org.xplore.project.data.local

import io.ktor.util.date.getTimeMillis
import org.xplore.project.data.local.db.XploreDatabase

/**
 * Local data source for managing offline visit queue.
 * Stores place IDs that were visited while the backend was unreachable,
 * so they can be synced later.
 */
class PendingVisitLocalDataSource(private val db: XploreDatabase) {

    private val queries get() = db.pendingVisitQueries

    /** Queue a visit for later sync. */
    fun insertVisit(placeId: String) {
        val now = getTimeMillis()
        queries.insertVisit(placeId, now)
    }

    /** Get all pending visit place IDs. */
    fun getAllPending(): List<String> =
        queries.selectAll().executeAsList().map { it.placeId }

    /** Remove a synced visit from the queue. */
    fun deleteVisit(placeId: String) {
        queries.deleteVisit(placeId)
    }

    /** Clear all pending visits. */
    fun clearAll() {
        queries.deleteAll()
    }
}

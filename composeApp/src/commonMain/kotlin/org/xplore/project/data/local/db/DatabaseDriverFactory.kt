package org.xplore.project.data.local.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific factory for creating the SQLDelight [SqlDriver].
 *
 * Each platform (Android, iOS) provides its own `actual` implementation
 * using the appropriate SQLite driver.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

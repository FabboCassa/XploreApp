package org.xplore.project.data.local.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS implementation: uses [NativeSqliteDriver] backed by the native SQLite library.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = XploreDatabase.Schema,
            name = "xplore.db",
        )
    }
}

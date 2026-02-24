package org.xplore.project.data.local.db

import android.app.Application
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Android implementation: uses [AndroidSqliteDriver] backed by the system SQLite.
 *
 * On schema version change, the old DB is DESTROYED and recreated.
 * This is acceptable because the data is just a cache of Overpass API results.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val context = XploreApplication.appContext

        // Destructive migration: if schema version changed, delete and recreate
        val currentVersion = XploreDatabase.Schema.version
        val prefs = context.getSharedPreferences("xplore_db_prefs", 0)
        val savedVersion = prefs.getLong("db_schema_version", -1L)

        if (savedVersion != -1L && savedVersion != currentVersion) {
            println("📱 [DB] Schema version changed ($savedVersion → $currentVersion). Deleting old database.")
            context.deleteDatabase("xplore.db")
        }
        prefs.edit().putLong("db_schema_version", currentVersion).apply()

        return AndroidSqliteDriver(
            schema = XploreDatabase.Schema,
            context = context,
            name = "xplore.db",
        )
    }
}

/**
 * Custom Application class that stores the application context.
 * Must be declared in AndroidManifest.xml.
 */
class XploreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        lateinit var appContext: Application
            private set
    }
}

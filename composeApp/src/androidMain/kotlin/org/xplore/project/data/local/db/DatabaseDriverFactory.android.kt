package org.xplore.project.data.local.db

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.xplore.project.R
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.xplore.project.di.appModule
import org.xplore.project.di.platformModule

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
        startKoin {
            androidContext(this@XploreApplication)
            modules(platformModule(), appModule)
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "xplore_default",
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var appContext: Application
            private set
    }
}

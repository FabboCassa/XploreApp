package org.xplore.project.di

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.xplore.project.data.local.MapPinLocalDataSource
import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.local.db.DatabaseDriverFactory
import org.xplore.project.data.local.db.XploreDatabase
import org.xplore.project.data.remote.AuthApiService
import org.xplore.project.data.remote.CommunityRemoteDataSource
import org.xplore.project.data.remote.MapPinRemoteDataSource
import org.xplore.project.data.remote.RadiusMetricsRemoteDataSource
import org.xplore.project.data.repository.AuthRepositoryImpl
import org.xplore.project.data.repository.CommunityRepositoryImpl
import org.xplore.project.data.repository.MuseumRepositoryImpl
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.CommunityRepository
import org.xplore.project.domain.repository.MuseumRepository
import org.xplore.project.network.createHttpClient
import org.xplore.project.ui.auth.AuthViewModel
import org.xplore.project.ui.community.CommunityViewModel
import org.xplore.project.ui.home.HomeViewModel
import org.xplore.project.ui.poi.PoiDetailViewModel
import org.xplore.project.ui.profile.ProfileViewModel

/**
 * Main Koin application module.
 *
 * Provides:
 * - **Network**: Ktor HttpClient with JSON serialization.
 * - **Database**: SQLDelight XploreDatabase for local POI caching.
 * - **Data**: Repository implementations, data sources, and token storage.
 * - **Presentation**: ViewModels for Home and Auth screens.
 *
 * Note: Location services (PermissionsController, LocationTracker) are created
 * at the Composable layer via moko-permissions-compose / moko-geo-compose
 * because they require platform-specific context (applicationContext on Android).
 */
val appModule = module {
    // ── Network ──────────────────────────────────────────────
    single {
        createHttpClient().config {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                    encodeDefaults = true
                })
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    // ── Auth API Service ─────────────────────────────────────
    single {
        AuthApiService(
            httpClient = get(),
            // Aspire redirects HTTP 5140 to HTTPS 7109. Using 7109 directly with bypassed SSL.
            baseUrl = "https://10.0.2.2:7109",
        )
    }

    // ── Local Storage ────────────────────────────────────────
    single { TokenManager() }

    // ── SQLDelight Database ──────────────────────────────────
    single { get<DatabaseDriverFactory>().createDriver() }
    single { XploreDatabase(get()) }

    // ── Data Sources ─────────────────────────────────────────
    single { MapPinLocalDataSource(get()) }
    single {
        MapPinRemoteDataSource(
            httpClient = get(),
            baseUrl = "https://10.0.2.2:7109",
        )
    }
    single {
        RadiusMetricsRemoteDataSource(
            httpClient = get(),
            baseUrl = "https://10.0.2.2:7109",
        )
    }

    // ── Data layer ───────────────────────────────────────────
    singleOf(::MuseumRepositoryImpl) bind MuseumRepository::class
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::CommunityRepositoryImpl) bind CommunityRepository::class

    // ── Community Data Source ─────────────────────────────────
    single {
        CommunityRemoteDataSource(
            httpClient = get(),
            baseUrl = "https://10.0.2.2:7109",
        )
    }

    // ── Presentation layer ───────────────────────────────────
    viewModelOf(::HomeViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::PoiDetailViewModel)
    viewModelOf(::CommunityViewModel)
}

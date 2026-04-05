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
import org.xplore.project.data.local.AppPreferences
import org.xplore.project.data.local.MapPinLocalDataSource
import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.local.db.DatabaseDriverFactory
import org.xplore.project.data.local.db.XploreDatabase
import org.xplore.project.data.remote.AuthApiService
import org.xplore.project.data.remote.CommunityRemoteDataSource
import org.xplore.project.data.remote.FriendApiService
import org.xplore.project.data.remote.NotificationApiService
import org.xplore.project.data.remote.MapPinRemoteDataSource
import org.xplore.project.data.remote.OsrmRoutingService
import org.xplore.project.data.remote.RadiusMetricsRemoteDataSource
import org.xplore.project.data.remote.RoutesRemoteDataSource
import org.xplore.project.data.repository.AuthRepositoryImpl
import org.xplore.project.data.repository.CommunityRepositoryImpl
import org.xplore.project.data.repository.FriendRepositoryImpl
import org.xplore.project.data.repository.MuseumRepositoryImpl
import org.xplore.project.data.repository.NotificationRepositoryImpl
import org.xplore.project.data.repository.SavedRouteRepositoryImpl
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.CommunityRepository
import org.xplore.project.domain.repository.FriendRepository
import org.xplore.project.domain.repository.MuseumRepository
import org.xplore.project.domain.repository.NotificationRepository
import org.xplore.project.domain.repository.SavedRouteRepository
import org.xplore.project.network.createHttpClient
import org.xplore.project.ui.app.AppViewModel
import org.xplore.project.ui.auth.AuthViewModel
import org.xplore.project.ui.community.CommunityViewModel
import org.xplore.project.ui.community.detail.CreateCompetitionViewModel
import org.xplore.project.ui.community.detail.GroupDetailViewModel
import org.xplore.project.ui.community.detail.PoiSelectionMapViewModel
import org.xplore.project.ui.community.detail.CompetitionMapViewModel
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
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.call.body
import org.xplore.project.data.remote.dto.TokenResponseDto

val appModule = module {
    // ── Network ──────────────────────────────────────────────
    // TODO: When the backend is deployed to a production server, replace ALL
    //  hardcoded "https://10.0.2.2:7109" URLs below (and in refreshTokens)
    //  with a BuildConfig / expect-actual based injection so that debug builds
    //  point to the emulator and release builds point to the real server.
    single {
        val tm = get<TokenManager>()
        createHttpClient().config {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                    encodeDefaults = true
                })
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(tm.accessToken ?: "", tm.refreshToken ?: "")
                    }
                    refreshTokens {
                        val token = tm.accessToken
                        val refresh = tm.refreshToken
                        if (token == null || refresh == null) return@refreshTokens null

                        try {
                            // Use a separate client to avoid infinite loops
                            val refreshClient = createHttpClient().config {
                                install(ContentNegotiation) {
                                    json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
                                }
                            }
                            val response = refreshClient.post("https://10.0.2.2:7109/api/auth/refresh") {
                                contentType(ContentType.Application.Json)
                                setBody(mapOf("accessToken" to token, "refreshToken" to refresh))
                            }
                            if (response.status.isSuccess()) {
                                val newTokens = response.body<TokenResponseDto>()
                                tm.saveTokens(newTokens.actualAccessToken ?: "", newTokens.actualRefreshToken ?: "", tm.isGuest)
                                BearerTokens(newTokens.actualAccessToken ?: "", newTokens.actualRefreshToken ?: "")
                            } else {
                                // Refresh rejected – session is expired
                                tm.triggerSessionExpired()
                                null
                            }
                        } catch (e: Exception) {
                            // Network error during refresh – treat as session expired
                            tm.triggerSessionExpired()
                            null
                        }
                    }
                }
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
    single { AppPreferences() }

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

    // ── Routing Service (OSRM) ──────────────────────────────
    single { OsrmRoutingService(httpClient = get()) }

    // ── Routes Data Source ────────────────────────────────────
    single {
        RoutesRemoteDataSource(
            httpClient = get(),
            baseUrl = "https://10.0.2.2:7109",
        )
    }

    // ── Data layer ───────────────────────────────────────────
    singleOf(::MuseumRepositoryImpl) bind MuseumRepository::class
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::CommunityRepositoryImpl) bind CommunityRepository::class
    singleOf(::NotificationRepositoryImpl) bind NotificationRepository::class
    singleOf(::SavedRouteRepositoryImpl) bind SavedRouteRepository::class

    // ── Community Data Source ─────────────────────────────────
    single {
        CommunityRemoteDataSource(
            httpClient = get(),
            baseUrl = "https://10.0.2.2:7109",
        )
    }

    // ── Notification API Service ─────────────────────────────
    single {
        NotificationApiService(
            httpClient = get(),
            baseUrl = "https://10.0.2.2:7109",
        )
    }

    // ── Friend Data Source & Repository ──────────────────────
    single {
        FriendApiService(
            httpClient = get(),
            baseUrl = "https://10.0.2.2:7109",
        )
    }
    singleOf(::FriendRepositoryImpl) bind FriendRepository::class

    // ── Presentation layer ───────────────────────────────────
    viewModelOf(::AppViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::PoiDetailViewModel)
    viewModelOf(::CommunityViewModel)
    viewModelOf(::GroupDetailViewModel)
    viewModelOf(::CreateCompetitionViewModel)
    viewModelOf(::PoiSelectionMapViewModel)
    viewModelOf(::CompetitionMapViewModel)
}

package org.xplore.project.di

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.remote.AuthApiService
import org.xplore.project.data.repository.AuthRepositoryImpl
import org.xplore.project.data.repository.MuseumRepositoryImpl
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.MuseumRepository
import org.xplore.project.ui.auth.AuthViewModel
import org.xplore.project.ui.home.HomeViewModel

/**
 * Main Koin application module.
 *
 * Provides:
 * - **Network**: Ktor HttpClient with JSON serialization.
 * - **Data**: Repository implementations and token storage.
 * - **Presentation**: ViewModels for Home and Auth screens.
 */
val appModule = module {
    // ── Network ──────────────────────────────────────────────
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                })
            }
        }
    }

    // ── Auth API Service ─────────────────────────────────────
    single {
        AuthApiService(
            httpClient = get(),
            // TODO: Move to BuildConfig / env config
            baseUrl = "http://10.0.2.2:5000",
        )
    }

    // ── Local Storage ────────────────────────────────────────
    single { TokenManager() }

    // ── Data layer ───────────────────────────────────────────
    singleOf(::MuseumRepositoryImpl) bind MuseumRepository::class
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class

    // ── Presentation layer ───────────────────────────────────
    viewModelOf(::HomeViewModel)
    viewModelOf(::AuthViewModel)
}

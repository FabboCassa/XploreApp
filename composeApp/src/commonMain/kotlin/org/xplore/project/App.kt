package org.xplore.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.xplore.project.data.local.AppPreferences
import org.xplore.project.data.local.ThemeMode
import org.xplore.project.di.appModule
import org.xplore.project.di.platformModule
import org.xplore.project.network.createHttpClient
import org.xplore.project.ui.navigation.XploreNavHost
import org.xplore.project.ui.theme.XploreTheme

/**
 * The main entry point composable for the Compose Multiplatform application.
 *
 * ## Architecture Role
 * This is the root of the Compose hierarchy. It is responsible for:
 * 1. **Dependency Injection**: Initializes Koin via [KoinApplication].
 * 2. **Design System**: Wraps the app in [XploreTheme] applying the user-selected or system theme.
 * 3. **Navigation**: Hosts [XploreNavHost] which manages all screen routing.
 *
 * ## Platform Integration
 * This composable is called directly from platform-specific entry points:
 * - Android: `MainActivity.kt` via `setContent { App() }`
 * - iOS: `MainViewController.kt` via `ComposeUIViewController { App() }`
 */
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = createHttpClient()))
            }
            .build()
    }

    KoinApplication(application = {
        modules(platformModule(), appModule)
    }) {
        val appPreferences = koinInject<AppPreferences>()
        val themeMode by appPreferences.themeModeFlow.collectAsState()
        val systemDark = isSystemInDarkTheme()
        val isDark = when (themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        XploreTheme(darkTheme = isDark) {
            XploreNavHost()
        }
    }
}

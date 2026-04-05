package org.xplore.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import org.koin.compose.koinInject
import org.xplore.project.data.local.AppPreferences
import org.xplore.project.data.local.LanguageMode
import org.xplore.project.data.local.ThemeMode
import org.xplore.project.network.createHttpClient
import org.xplore.project.platform.applyLocaleOverride
import org.xplore.project.platform.getDeviceLanguageTag
import org.xplore.project.ui.navigation.XploreNavHost
import org.xplore.project.ui.theme.XploreTheme

/**
 * Platform-specific Koin wrapper.
 * - Android: wraps with [KoinContext] (Koin already started in XploreApplication)
 * - iOS: wraps with [KoinApplication] (starts Koin here)
 */
@Composable
expect fun KoinWrapper(content: @Composable () -> Unit)

/**
 * The main entry point composable for the Compose Multiplatform application.
 *
 * ## Architecture Role
 * This is the root of the Compose hierarchy. It is responsible for:
 * 1. **Dependency Injection**: Delegates Koin setup to [KoinWrapper].
 * 2. **Design System**: Wraps the app in [XploreTheme] applying the user-selected or system theme.
 * 3. **Localization**: Applies the user-selected language or device default.
 * 4. **Navigation**: Hosts [XploreNavHost] which manages all screen routing.
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

    KoinWrapper {
        val appPreferences = koinInject<AppPreferences>()
        val themeMode by appPreferences.themeModeFlow.collectAsState()
        val languageMode by appPreferences.languageModeFlow.collectAsState()

        // ── Resolve effective locale tag ──
        val effectiveTag = if (languageMode == LanguageMode.SYSTEM) {
            val deviceTag = getDeviceLanguageTag()
            val supported = LanguageMode.entries
                .filter { it != LanguageMode.SYSTEM }
                .map { it.tag }
            if (deviceTag in supported) deviceTag else "en"
        } else {
            languageMode.tag
        }

        // Apply the locale at the platform level so Compose Resources
        // resolves the correct values-<tag>/strings.xml
        LaunchedEffect(effectiveTag) {
            applyLocaleOverride(effectiveTag)
        }

        val systemDark = isSystemInDarkTheme()
        val isDark = when (themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        // key(effectiveTag) forces a full reload of the screen when
        // the language changes, so all stringResource() calls pick up
        // the new locale immediately.
        key(effectiveTag) {
            XploreTheme(darkTheme = isDark) {
                XploreNavHost()
            }
        }
    }
}

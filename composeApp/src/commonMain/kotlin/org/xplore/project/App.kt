package org.xplore.project

import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication
import org.xplore.project.di.appModule
import org.xplore.project.di.platformModule
import org.xplore.project.ui.navigation.XploreNavHost
import org.xplore.project.ui.theme.XploreTheme

/**
 * The main entry point composable for the Compose Multiplatform application.
 *
 * ## Architecture Role
 * This is the root of the Compose hierarchy. It is responsible for:
 * 1. **Dependency Injection**: Initializes Koin via [KoinApplication].
 * 2. **Design System**: Wraps the app in [XploreTheme] to apply colors, typography, and shapes.
 * 3. **Navigation**: Hosts [XploreNavHost] which manages all screen routing.
 *
 * ## Platform Integration
 * This composable is called directly from platform-specific entry points:
 * - Android: `MainActivity.kt` via `setContent { App() }`
 * - iOS: `MainViewController.kt` via `ComposeUIViewController { App() }`
 */
@Composable
fun App() {
    KoinApplication(application = {
        modules(platformModule(), appModule)
    }) {
        XploreTheme {
            XploreNavHost()
        }
    }
}
package org.xplore.project.ui.util

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that provides a callback to request notification permission.
 * Set by the platform-specific Activity/ViewController.
 * Default no-op for platforms that don't need explicit permission requests.
 */
val LocalNotificationPermissionRequester = staticCompositionLocalOf<() -> Unit> { {} }

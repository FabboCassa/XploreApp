package org.xplore.project.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.xplore.project.data.local.AppPreferences
import org.xplore.project.data.local.ThemeMode
import org.xplore.project.data.local.TokenManager
import org.xplore.project.domain.repository.NotificationRepository
import org.xplore.project.getCurrentFcmToken

/**
 * Lightweight ViewModel that exposes theme preference for UI screens.
 * Delegates to [AppPreferences.themeModeFlow] so that every observer
 * — including [App.kt] — recomposes immediately on change.
 *
 * Also registers the FCM device token with the backend on startup
 * (only when the user is authenticated).
 */
class AppViewModel(
    private val appPreferences: AppPreferences,
    private val tokenManager: TokenManager,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeModeFlow

    init {
        registerFcmTokenIfLoggedIn()
    }

    fun setThemeMode(mode: ThemeMode) {
        appPreferences.themeMode = mode
    }

    /**
     * Obtains the current FCM token and registers it with the backend.
     * Skipped silently if the user is not logged in or token is unavailable.
     */
    fun registerFcmTokenIfLoggedIn() {
        val isLoggedIn = tokenManager.accessToken != null && !tokenManager.isGuest
        if (!isLoggedIn) return

        viewModelScope.launch {
            val token = getCurrentFcmToken() ?: return@launch
            runCatching {
                notificationRepository.registerDeviceToken(token, "android")
            }
        }
    }
}

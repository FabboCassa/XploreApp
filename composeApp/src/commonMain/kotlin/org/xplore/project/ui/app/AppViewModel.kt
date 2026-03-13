package org.xplore.project.ui.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.xplore.project.data.local.AppPreferences
import org.xplore.project.data.local.ThemeMode

/**
 * Lightweight ViewModel that exposes theme preference for UI screens.
 * Delegates to [AppPreferences.themeModeFlow] so that every observer
 * — including [App.kt] — recomposes immediately on change.
 */
class AppViewModel(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeModeFlow

    fun setThemeMode(mode: ThemeMode) {
        appPreferences.themeMode = mode
    }
}

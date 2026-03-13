package org.xplore.project.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Persists user-facing app preferences (theme mode, etc.).
 *
 * Uses [Settings] (multiplatform-settings) which maps to:
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 *
 * Exposes [themeModeFlow] so that any composable observing it
 * recomposes immediately when the theme changes — even across
 * different ViewModel scopes.
 */
class AppPreferences {

    private val settings = Settings()

    private val _themeModeFlow = MutableStateFlow(readThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    var themeMode: ThemeMode
        get() = _themeModeFlow.value
        set(value) {
            settings.putString(KEY_THEME_MODE, value.name)
            _themeModeFlow.value = value
        }

    private fun readThemeMode(): ThemeMode =
        ThemeMode.entries.find { it.name == settings.getStringOrNull(KEY_THEME_MODE) }
            ?: ThemeMode.SYSTEM

    companion object {
        private const val KEY_THEME_MODE = "xplore_theme_mode"
    }
}

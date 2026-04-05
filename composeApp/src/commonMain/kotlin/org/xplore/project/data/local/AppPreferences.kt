package org.xplore.project.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Supported app languages.
 *
 * [SYSTEM] follows the device locale with English as fallback.
 * Each explicit entry maps to a BCP-47 language tag used by
 * Compose Multiplatform resources (`values-<tag>/strings.xml`).
 */
enum class LanguageMode(val tag: String, val displayName: String) {
    SYSTEM("system", "Auto"),
    EN("en", "English"),
    IT("it", "Italiano"),
    FR("fr", "Français"),
    DE("de", "Deutsch"),
    ES("es", "Español"),
    PT("pt", "Português"),
    ZH("zh", "中文"),
    JA("ja", "日本語"),
}

/**
 * Persists user-facing app preferences (theme mode, language, etc.).
 *
 * Uses [Settings] (multiplatform-settings) which maps to:
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 *
 * Exposes [themeModeFlow] and [languageModeFlow] so that any composable
 * observing them recomposes immediately when the preference changes —
 * even across different ViewModel scopes.
 */
class AppPreferences {

    private val settings = Settings()

    // ── Theme ───────────────────────────────────────────────────

    private val _themeModeFlow = MutableStateFlow(readThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    var themeMode: ThemeMode
        get() = _themeModeFlow.value
        set(value) {
            settings.putString(KEY_THEME_MODE, value.name)
            _themeModeFlow.value = value
        }

    // ── Language ─────────────────────────────────────────────────

    private val _languageModeFlow = MutableStateFlow(readLanguageMode())
    val languageModeFlow: StateFlow<LanguageMode> = _languageModeFlow.asStateFlow()

    var languageMode: LanguageMode
        get() = _languageModeFlow.value
        set(value) {
            settings.putString(KEY_LANGUAGE_MODE, value.name)
            _languageModeFlow.value = value
        }

    // ── Notifications ───────────────────────────────────────────

    private val _notificationsEnabledFlow = MutableStateFlow(
        settings.getBoolean(KEY_NOTIFICATIONS, false)
    )
    val notificationsEnabledFlow: StateFlow<Boolean> = _notificationsEnabledFlow.asStateFlow()

    var notificationsEnabled: Boolean
        get() = _notificationsEnabledFlow.value
        set(value) {
            settings.putBoolean(KEY_NOTIFICATIONS, value)
            _notificationsEnabledFlow.value = value
        }

    var notificationsAsked: Boolean
        get() = settings.getBoolean(KEY_NOTIFICATIONS_ASKED, false)
        set(value) {
            settings.putBoolean(KEY_NOTIFICATIONS_ASKED, value)
        }

    // ── Helpers ──────────────────────────────────────────────────

    private fun readThemeMode(): ThemeMode =
        ThemeMode.entries.find { it.name == settings.getStringOrNull(KEY_THEME_MODE) }
            ?: ThemeMode.SYSTEM

    private fun readLanguageMode(): LanguageMode =
        LanguageMode.entries.find { it.name == settings.getStringOrNull(KEY_LANGUAGE_MODE) }
            ?: LanguageMode.SYSTEM

    companion object {
        private const val KEY_THEME_MODE = "xplore_theme_mode"
        private const val KEY_LANGUAGE_MODE = "xplore_language_mode"
        private const val KEY_NOTIFICATIONS = "xplore_notifications_enabled"
        private const val KEY_NOTIFICATIONS_ASKED = "xplore_notifications_asked"
    }
}

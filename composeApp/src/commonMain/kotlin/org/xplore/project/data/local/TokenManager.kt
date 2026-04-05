package org.xplore.project.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manages JWT token persistence across app sessions.
 *
 * Uses [Settings] (multiplatform-settings) which maps to:
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 *
 * ## Security Note
 * For production, consider migrating to encrypted storage
 * (EncryptedSharedPreferences on Android, Keychain on iOS).
 * For the MVP, plain Settings is acceptable.
 */
class TokenManager {

    private val settings = Settings()

    /** Emitted when a token refresh fails, signalling that the user must re-login. */
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    /** Call from the HTTP-client refresh logic when the refresh token is rejected. */
    fun triggerSessionExpired() {
        clear()
        _sessionExpiredEvent.tryEmit(Unit)
    }

    var accessToken: String?
        get() = settings.getStringOrNull(KEY_ACCESS_TOKEN)
        set(value) {
            if (value != null) settings.putString(KEY_ACCESS_TOKEN, value)
            else settings.remove(KEY_ACCESS_TOKEN)
        }

    var refreshToken: String?
        get() = settings.getStringOrNull(KEY_REFRESH_TOKEN)
        set(value) {
            if (value != null) settings.putString(KEY_REFRESH_TOKEN, value)
            else settings.remove(KEY_REFRESH_TOKEN)
        }

    val isLoggedIn: Boolean
        get() = accessToken != null

    val isGuest: Boolean
        get() = settings.getBoolean(KEY_IS_GUEST, false)

    fun saveTokens(access: String, refresh: String, isGuest: Boolean = false) {
        accessToken = access
        refreshToken = refresh
        settings.putBoolean(KEY_IS_GUEST, isGuest)
    }

    fun clear() {
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_IS_GUEST)
    }

    // ── Radius settings (persisted across restarts) ────────────
    var searchRadiusKm: Double
        get() = settings.getDouble(KEY_SEARCH_RADIUS, 3.0)
        set(value) = settings.putDouble(KEY_SEARCH_RADIUS, value)

    /** Epoch millis when the current radius was set. */
    var radiusSetAtMs: Long
        get() = settings.getLong(KEY_RADIUS_SET_AT, 0L)
        set(value) = settings.putLong(KEY_RADIUS_SET_AT, value)

    // ── Map State ────────────
    var lastMapLatitude: Double?
        get() = if (settings.hasKey(KEY_LAST_MAP_LAT)) settings.getDouble(KEY_LAST_MAP_LAT, 0.0) else null
        set(value) {
            if (value != null) settings.putDouble(KEY_LAST_MAP_LAT, value)
            else settings.remove(KEY_LAST_MAP_LAT)
        }

    var lastMapLongitude: Double?
        get() = if (settings.hasKey(KEY_LAST_MAP_LNG)) settings.getDouble(KEY_LAST_MAP_LNG, 0.0) else null
        set(value) {
            if (value != null) settings.putDouble(KEY_LAST_MAP_LNG, value)
            else settings.remove(KEY_LAST_MAP_LNG)
        }

    companion object {
        private const val KEY_ACCESS_TOKEN = "xplore_access_token"
        private const val KEY_REFRESH_TOKEN = "xplore_refresh_token"
        private const val KEY_IS_GUEST = "xplore_is_guest"
        private const val KEY_SEARCH_RADIUS = "xplore_search_radius"
        private const val KEY_RADIUS_SET_AT = "xplore_radius_set_at"
        private const val KEY_LAST_MAP_LAT = "xplore_last_map_lat"
        private const val KEY_LAST_MAP_LNG = "xplore_last_map_lng"
    }
}

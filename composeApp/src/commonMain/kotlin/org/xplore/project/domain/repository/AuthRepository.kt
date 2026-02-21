package org.xplore.project.domain.repository

import org.xplore.project.data.remote.dto.UserInfoDto

/**
 * Repository contract for authentication operations.
 *
 * ## Result Pattern
 * All methods return [Result] to cleanly propagate success/failure
 * without exceptions leaking into the presentation layer.
 */
interface AuthRepository {
    /** Login with email and password. Returns true on success. */
    suspend fun login(email: String, password: String): Result<Boolean>

    /** Register a personal account. Returns true on success. */
    suspend fun register(email: String, password: String, userName: String): Result<Boolean>

    /** Create a guest session (no credentials needed). Returns true on success. */
    suspend fun guestLogin(): Result<Boolean>

    /** Get the current user's info. Requires an active session. */
    suspend fun getCurrentUser(): Result<UserInfoDto>

    /** Clear stored tokens and sign out. */
    fun logout()

    /** Check if there's a stored session (token present). */
    fun isLoggedIn(): Boolean

    /** Check if current session is a guest session. */
    fun isGuest(): Boolean
}

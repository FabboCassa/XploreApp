package org.xplore.project.domain.repository

import org.xplore.project.data.remote.dto.UserInfoDto

sealed class AuthResult {
    data object Success : AuthResult()
    data class RequiresTwoFactor(val userId: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Repository contract for authentication operations.
 *
 * ## Result Pattern
 * Methods return [AuthResult] or [Result] to cleanly propagate success/failure
 * without exceptions leaking into the presentation layer.
 */
interface AuthRepository {
    /** Login with email and password. */
    suspend fun login(email: String, password: String): AuthResult

    /** Register a personal account. Returns true on success. */
    suspend fun register(email: String, password: String, userName: String): Result<Boolean>

    /** Create a guest session (no credentials needed). Returns true on success. */
    suspend fun guestLogin(): AuthResult

    /** Login via external provider (Google, Apple). */
    suspend fun externalLogin(provider: String, idToken: String): AuthResult

    /** Verify 2FA code. */
    suspend fun verifyTwoFactor(userId: String, code: String, provider: String = "Email"): AuthResult

    /** Setup 2FA. Returns SharedKey and AuthenticatorUri. */
    suspend fun setupTwoFactor(): Result<Pair<String, String>>

    /** Get the current user's info. Requires an active session. */
    suspend fun getCurrentUser(): Result<UserInfoDto>

    /** Clear stored tokens and sign out. */
    fun logout()

    /** Check if there's a stored session (token present). */
    fun isLoggedIn(): Boolean

    /** Check if current session is a guest session. */
    fun isGuest(): Boolean
}

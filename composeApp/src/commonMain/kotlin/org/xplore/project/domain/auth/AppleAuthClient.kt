package org.xplore.project.domain.auth

import androidx.compose.runtime.Composable

/**
 * Client interface for handling Apple Sign-In across platforms.
 */
interface AppleAuthClient {
    suspend fun signIn(): AppleSignInResult
}

sealed class AppleSignInResult {
    data class Success(val identityToken: String) : AppleSignInResult()
    data class Error(val message: String) : AppleSignInResult()
    data object Cancelled : AppleSignInResult()
}

/**
 * Creates and remembers a platform-specific [AppleAuthClient].
 */
@Composable
expect fun rememberAppleAuthClient(): AppleAuthClient

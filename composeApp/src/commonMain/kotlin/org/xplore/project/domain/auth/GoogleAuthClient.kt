package org.xplore.project.domain.auth

import androidx.compose.runtime.Composable

/**
 * Client interface for handling Google Sign-In across platforms.
 */
interface GoogleAuthClient {
    suspend fun signIn(): GoogleSignInResult
}

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
}

/**
 * Creates and remembers a platform-specific [GoogleAuthClient].
 */
@Composable
expect fun rememberGoogleAuthClient(): GoogleAuthClient

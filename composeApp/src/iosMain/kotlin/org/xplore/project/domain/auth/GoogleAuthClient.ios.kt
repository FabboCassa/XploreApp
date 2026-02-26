package org.xplore.project.domain.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class IosGoogleAuthClient : GoogleAuthClient {
    override suspend fun signIn(): GoogleSignInResult {
        return GoogleSignInResult.Error("Google Sign-In non ancora implementato su iOS")
    }
}

@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    return remember { IosGoogleAuthClient() }
}

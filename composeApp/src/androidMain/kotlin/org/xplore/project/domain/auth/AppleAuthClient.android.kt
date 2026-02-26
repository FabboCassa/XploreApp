package org.xplore.project.domain.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class AndroidAppleAuthClient : AppleAuthClient {
    override suspend fun signIn(): AppleSignInResult {
        return AppleSignInResult.Error("Accedi con Apple non è supportato su Android.")
    }
}

@Composable
actual fun rememberAppleAuthClient(): AppleAuthClient {
    return remember { AndroidAppleAuthClient() }
}

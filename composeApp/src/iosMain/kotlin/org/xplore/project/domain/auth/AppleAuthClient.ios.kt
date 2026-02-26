package org.xplore.project.domain.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASPresentationAnchor
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosAppleAuthClient : AppleAuthClient {
    
    override suspend fun signIn(): AppleSignInResult = suspendCancellableCoroutine { continuation ->
        val provider = ASAuthorizationAppleIDProvider()
        val request = provider.createRequest()
        val controller = ASAuthorizationController(listOf(request))
        
        val delegate = object : NSObject(), ASAuthorizationControllerDelegateProtocol, ASAuthorizationControllerPresentationContextProvidingProtocol {
            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithAuthorization: ASAuthorization
            ) {
                val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
                val identityTokenData = credential?.identityToken
                if (identityTokenData != null) {
                    val tokenString = identityTokenData.toKString()
                    continuation.resume(AppleSignInResult.Success(tokenString))
                } else {
                    continuation.resume(AppleSignInResult.Error("Nessun token di identità ricevuto da Apple"))
                }
            }

            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithError: platform.Foundation.NSError
            ) {
                // ASAuthorizationErrorCanceled = 1001
                if (didCompleteWithError.code == 1001L) {
                    continuation.resume(AppleSignInResult.Cancelled)
                } else {
                    continuation.resume(AppleSignInResult.Error(didCompleteWithError.localizedDescription))
                }
            }

            override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
                val scenes = platform.UIKit.UIApplication.sharedApplication.connectedScenes
                val windowScene = scenes.firstOrNull() as? platform.UIKit.UIWindowScene
                return windowScene?.windows?.firstOrNull() ?: platform.UIKit.UIWindow()
            }
        }
        
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        controller.performRequests()
        
        continuation.invokeOnCancellation {
            // Cancel requests if the coroutine is cancelled (though not natively supported by ASAuthorizationController)
        }
    }
    
    private fun NSData.toKString(): String {
        if (length == 0uL) return ""
        val bytes = ByteArray(length.toInt())
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), bytes, length)
        }
        return bytes.decodeToString()
    }
}

@Composable
actual fun rememberAppleAuthClient(): AppleAuthClient {
    return remember { IosAppleAuthClient() }
}

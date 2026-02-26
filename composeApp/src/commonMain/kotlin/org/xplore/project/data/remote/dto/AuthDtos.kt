package org.xplore.project.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ── Request DTOs ─────────────────────────────────────────────

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterPersonalRequestDto(
    val email: String,
    val password: String,
    val userName: String,
)

@Serializable
data class ExternalLoginRequestDto(
    val provider: String,  // "Google" | "Apple"
    val idToken: String,
)

@Serializable
data class SendEmail2FARequestDto(
    val userId: String,
)

@Serializable
data class TwoFactorVerifyRequestDto(
    val userId: String,
    val code: String,
    val provider: String = "Email", // or "Authenticator"
)

// ── Response DTOs ────────────────────────────────────────────

@Serializable
data class TokenResponseDto(
    @SerialName("AccessToken") val accessToken: String? = null,
    @SerialName("RefreshToken") val refreshToken: String? = null,
    @SerialName("ExpiresAt") val expiresAt: String? = null,
    @SerialName("accessToken") val accessTokenFallback: String? = null,
    @SerialName("refreshToken") val refreshTokenFallback: String? = null,
    @SerialName("expiresAt") val expiresAtFallback: String? = null,
    val requiresTwoFactor: Boolean = false,
    val userId: String? = null,
) {
    val actualAccessToken: String? get() = accessToken ?: accessTokenFallback
    val actualRefreshToken: String? get() = refreshToken ?: refreshTokenFallback
}

@Serializable
data class TwoFactorSetupResponseDto(
    @SerialName("SharedKey") val sharedKey: String? = null,
    @SerialName("AuthenticatorUri") val authenticatorUri: String? = null,
    @SerialName("sharedKey") val sharedKeyFallback: String? = null,
    @SerialName("authenticatorUri") val authenticatorUriFallback: String? = null,
) {
    val actualSharedKey: String get() = sharedKey ?: sharedKeyFallback ?: ""
    val actualAuthenticatorUri: String get() = authenticatorUri ?: authenticatorUriFallback ?: ""
}

@Serializable
data class AuthResponseDto(
    val success: Boolean,
    val message: String,
    val errors: List<String>? = null,
)

@Serializable
data class UserInfoDto(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val accountType: String,
    val isPremium: Boolean,
    val museumId: String? = null,
    val companyName: String? = null,
    val roles: List<String> = emptyList(),
)

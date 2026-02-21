package org.xplore.project.data.remote.dto

import kotlinx.serialization.Serializable

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

// ── Response DTOs ────────────────────────────────────────────

@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String,
)

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

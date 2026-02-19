package org.xplore.project.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.xplore.project.data.remote.dto.AuthResponseDto
import org.xplore.project.data.remote.dto.LoginRequestDto
import org.xplore.project.data.remote.dto.RegisterPersonalRequestDto
import org.xplore.project.data.remote.dto.TokenResponseDto
import org.xplore.project.data.remote.dto.UserInfoDto

/**
 * Ktor-based HTTP client for the Xplore Auth API.
 *
 * @param httpClient Pre-configured Ktor HttpClient with JSON serialization.
 * @param baseUrl Base URL of the Xplore backend (e.g., "http://10.0.2.2:5000").
 */
class AuthApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun login(request: LoginRequestDto): TokenResponseDto {
        return httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun registerPersonal(request: RegisterPersonalRequestDto): AuthResponseDto {
        return httpClient.post("$baseUrl/api/auth/register/personal") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun guestLogin(): TokenResponseDto {
        return httpClient.post("$baseUrl/api/auth/guest").body()
    }

    suspend fun refreshToken(accessToken: String, refreshToken: String): TokenResponseDto {
        return httpClient.post("$baseUrl/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("accessToken" to accessToken, "refreshToken" to refreshToken))
        }.body()
    }

    suspend fun getCurrentUser(token: String): UserInfoDto {
        return httpClient.get("$baseUrl/api/auth/me") {
            bearerAuth(token)
        }.body()
    }
}

package org.xplore.project.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.xplore.project.data.remote.dto.AuthResponseDto
import org.xplore.project.data.remote.dto.LoginRequestDto
import org.xplore.project.data.remote.dto.RegisterPersonalRequestDto
import org.xplore.project.data.remote.dto.TokenResponseDto
import org.xplore.project.data.remote.dto.TwoFactorSetupResponseDto
import org.xplore.project.data.remote.dto.TwoFactorVerifyRequestDto
import org.xplore.project.data.remote.dto.UserInfoDto
import io.ktor.client.statement.*
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

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
        }.handleResponse()
    }

    suspend fun registerPersonal(request: RegisterPersonalRequestDto): AuthResponseDto {
        return httpClient.post("$baseUrl/api/auth/register/personal") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }

    suspend fun guestLogin(): TokenResponseDto {
        return httpClient.post("$baseUrl/api/auth/guest").handleResponse()
    }

    suspend fun externalLogin(request: org.xplore.project.data.remote.dto.ExternalLoginRequestDto): TokenResponseDto {
        return httpClient.post("$baseUrl/api/auth/external-login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }

    suspend fun refreshToken(accessToken: String, refreshToken: String): TokenResponseDto {
        return httpClient.post("$baseUrl/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("accessToken" to accessToken, "refreshToken" to refreshToken))
        }.handleResponse()
    }

    suspend fun verifyTwoFactor(request: TwoFactorVerifyRequestDto): TokenResponseDto {
        return httpClient.post("$baseUrl/api/auth/2fa/verify") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }

    suspend fun setupTwoFactor(token: String): TwoFactorSetupResponseDto {
        return httpClient.post("$baseUrl/api/auth/2fa/setup") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.handleResponse()
    }

    suspend fun sendEmail2Fa(request: org.xplore.project.data.remote.dto.SendEmail2FARequestDto): AuthResponseDto {
        return httpClient.post("$baseUrl/api/auth/2fa/send-email") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }

    suspend fun getCurrentUser(token: String): UserInfoDto {
        return httpClient.get("$baseUrl/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.handleResponse()
    }
    
    private suspend inline fun <reified T> HttpResponse.handleResponse(): T {
        if (!status.isSuccess()) {
            val errorText = try { bodyAsText() } catch (e: Exception) { "" }
            var errorMsg = "HTTP Error ${status.value}"
            try {
                if (errorText.isNotBlank()) {
                    val errorObj = Json { ignoreUnknownKeys = true }.decodeFromString<org.xplore.project.data.remote.dto.AuthResponseDto>(errorText)
                    errorMsg = errorObj.message
                } else if (status.value == 401) {
                    errorMsg = "Non autorizzato (401)"
                }
            } catch (e: Exception) {
                errorMsg = "HTTP Error ${status.value}"
            }
            throw Exception(errorMsg)
        }
        return body()
    }
}

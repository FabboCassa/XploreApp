package org.xplore.project.data.repository

import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.remote.AuthApiService
import org.xplore.project.data.remote.dto.LoginRequestDto
import org.xplore.project.data.remote.dto.RegisterPersonalRequestDto
import org.xplore.project.data.remote.dto.UserInfoDto
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.AuthResult

/**
 * Concrete implementation of [AuthRepository].
 *
 * Coordinates between the network ([AuthApiService]) and
 * local storage ([TokenManager]) to manage authentication state.
 */
class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager,
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = authApiService.login(LoginRequestDto(email, password))
            if (response.requiresTwoFactor && response.userId != null) {
                return AuthResult.RequiresTwoFactor(response.userId)
            }
            tokenManager.saveTokens(
                access = response.actualAccessToken ?: "",
                refresh = response.actualRefreshToken ?: "",
                isGuest = false,
            )
            AuthResult.Success
        } catch (e: Exception) {
            println("AuthError: ${e.message}")
            AuthResult.Error(e.message ?: "Login failed")
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        userName: String,
    ): Result<Boolean> {
        return try {
            authApiService.registerPersonal(
                RegisterPersonalRequestDto(email, password, userName)
            )
            // Auto-login after registration
            val loginResult = login(email, password)
            if (loginResult is AuthResult.Success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Login after registration failed"))
            }
        } catch (e: Exception) {
            println("AuthError [register]: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun guestLogin(): AuthResult {
        return try {
            val response = authApiService.guestLogin()
            tokenManager.saveTokens(
                access = response.actualAccessToken ?: "",
                refresh = response.actualRefreshToken ?: "",
                isGuest = true,
            )
            AuthResult.Success
        } catch (e: Exception) {
            println("AuthError [guestLogin]: ${e.message}")
            // Server unreachable — create a local-only offline guest session
            // so the user can still explore with cached / Overpass-fallback POIs.
            println("🔌 [Auth] Server offline — creating local guest session")
            tokenManager.saveTokens(
                access = "offline_guest",
                refresh = "offline_guest",
                isGuest = true,
            )
            AuthResult.Success
        }
    }

    override suspend fun externalLogin(provider: String, idToken: String): AuthResult {
        return try {
            val response = authApiService.externalLogin(
                org.xplore.project.data.remote.dto.ExternalLoginRequestDto(provider, idToken)
            )
            if (response.requiresTwoFactor && response.userId != null) {
                return AuthResult.RequiresTwoFactor(response.userId)
            }
            tokenManager.saveTokens(
                access = response.actualAccessToken ?: "",
                refresh = response.actualRefreshToken ?: "",
                isGuest = false,
            )
            AuthResult.Success
        } catch (e: Exception) {
            println("AuthError [externalLogin]: ${e.message}")
            AuthResult.Error(e.message ?: "External login failed")
        }
    }

    override suspend fun verifyTwoFactor(userId: String, code: String, provider: String): AuthResult {
        return try {
            val response = authApiService.verifyTwoFactor(
                org.xplore.project.data.remote.dto.TwoFactorVerifyRequestDto(userId, code, provider)
            )
            tokenManager.saveTokens(
                access = response.actualAccessToken ?: "",
                refresh = response.actualRefreshToken ?: "",
                isGuest = false,
            )
            AuthResult.Success
        } catch (e: Exception) {
            println("AuthError [verifyTwoFactor]: ${e.message}")
            AuthResult.Error(e.message ?: "2FA verification failed")
        }
    }

    override suspend fun setupTwoFactor(): Result<Pair<String, String>> {
        return try {
            val token = tokenManager.accessToken
                ?: return Result.failure(Exception("Not authenticated"))
            val response = authApiService.setupTwoFactor(token)
            Result.success(Pair(response.actualSharedKey, response.actualAuthenticatorUri))
        } catch (e: Exception) {
            println("AuthError [setupTwoFactor]: ${e.message}")
            if (e.message?.contains("NoTransformationFoundException") == true || e.toString().contains("NoTransformationFoundException") || e.message?.contains("401") == true) {
                Result.failure(Exception("Sessione scaduta. Effettua il logout e accedi nuovamente."))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun sendEmail2Fa(userId: String): Result<Boolean> {
        return try {
            authApiService.sendEmail2Fa(
                org.xplore.project.data.remote.dto.SendEmail2FARequestDto(userId)
            )
            Result.success(true)
        } catch (e: Exception) {
            println("AuthError [sendEmail2Fa]: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<UserInfoDto> {
        return try {
            val token = tokenManager.accessToken
                ?: return Result.failure(Exception("Not authenticated"))
            val user = authApiService.getCurrentUser(token)
            Result.success(user)
        } catch (e: Exception) {
            println("AuthError [getCurrentUser]: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, fileName: String): Result<UserInfoDto> {
        return try {
            val token = tokenManager.accessToken
                ?: return Result.failure(Exception("Not authenticated"))
            val user = authApiService.uploadAvatar(token, imageBytes, fileName)
            Result.success(user)
        } catch (e: Exception) {
            println("AuthError [uploadAvatar]: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteAvatar(): Result<Boolean> {
        return try {
            val token = tokenManager.accessToken
                ?: return Result.failure(Exception("Not authenticated"))
            authApiService.deleteAvatar(token)
            Result.success(true)
        } catch (e: Exception) {
            println("AuthError [deleteAvatar]: ${e.message}")
            Result.failure(e)
        }
    }

    override fun logout() {
        tokenManager.clear()
    }

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn

    override fun isGuest(): Boolean = tokenManager.isGuest

    override fun getAvatarUrl(userId: String): String {
        return "${authApiService.baseUrl}/api/auth/avatar/$userId"
    }
}

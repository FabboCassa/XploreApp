package org.xplore.project.data.repository

import org.xplore.project.data.local.TokenManager
import org.xplore.project.data.remote.AuthApiService
import org.xplore.project.data.remote.dto.LoginRequestDto
import org.xplore.project.data.remote.dto.RegisterPersonalRequestDto
import org.xplore.project.data.remote.dto.UserInfoDto
import org.xplore.project.domain.repository.AuthRepository

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

    override suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            val response = authApiService.login(LoginRequestDto(email, password))
            tokenManager.saveTokens(
                access = response.accessToken,
                refresh = response.refreshToken,
                isGuest = false,
            )
            Result.success(true)
        } catch (e: Exception) {
            println("AuthError [login]: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
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
            login(email, password)
        } catch (e: Exception) {
            println("AuthError [register]: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun guestLogin(): Result<Boolean> {
        return try {
            val response = authApiService.guestLogin()
            tokenManager.saveTokens(
                access = response.accessToken,
                refresh = response.refreshToken,
                isGuest = true,
            )
            Result.success(true)
        } catch (e: Exception) {
            println("AuthError [guestLogin]: ${e.message}")
            e.printStackTrace()
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
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun logout() {
        tokenManager.clear()
    }

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn

    override fun isGuest(): Boolean = tokenManager.isGuest
}

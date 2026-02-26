package org.xplore.project.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.AuthResult
import xploreapp.composeapp.generated.resources.*

/**
 * ViewModel shared across all auth screens (Welcome, Login, Register).
 *
 * ## State Management
 * Exposes [AuthUiState] which tracks form fields, loading, and error states.
 * Uses UDF: UI events → ViewModel methods → state updates → UI recomposition.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Whether the user has an existing session (for auto-skip to Home). */
    val isAlreadyLoggedIn: Boolean
        get() = authRepository.isLoggedIn()

    // ── Login ────────────────────────────────────────────────

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onLoginClicked() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = Res.string.error_missing_email_password) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.login(state.email, state.password)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is AuthResult.RequiresTwoFactor -> {
                    _uiState.update { it.copy(isLoading = false, requiresTwoFactorUserId = result.userId) }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = Res.string.error_invalid_credentials,
                        )
                    }
                }
            }
        }
    }

    // ── Guest ────────────────────────────────────────────────

    fun onGuestAccess() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.guestLogin()) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is AuthResult.RequiresTwoFactor -> {
                    // Guests shouldn't require 2FA, but just in case
                    _uiState.update { it.copy(isLoading = false, requiresTwoFactorUserId = result.userId) }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = Res.string.error_guest_failed,
                        )
                    }
                }
            }
        }
    }

    // ── Register ─────────────────────────────────────────────

    fun onUserNameChanged(name: String) {
        _uiState.update { it.copy(userName = name, errorMessage = null) }
    }

    fun onRegisterClicked() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.userName.isBlank()) {
            _uiState.update { it.copy(errorMessage = Res.string.error_missing_fields) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.register(state.email, state.password, state.userName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = Res.string.error_register_failed,
                        )
                    }
                },
            )
        }
    }

    // ── Social Login (Prepared for backend) ──────────────

    fun onExternalLoginSuccess(provider: String, idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.externalLogin(provider, idToken)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is AuthResult.RequiresTwoFactor -> {
                    _uiState.update { it.copy(isLoading = false, requiresTwoFactorUserId = result.userId) }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = Res.string.error_invalid_credentials,
                        )
                    }
                }
            }
        }
    }

    fun onExternalLoginError(provider: String, message: String) {
        println("External Login Error [$provider]: $message")
        _uiState.update { 
            val res = if (provider == "Google") Res.string.error_google_unavailable else Res.string.error_apple_unavailable
            it.copy(isLoading = false, errorMessage = res) 
        }
    }

    fun onGoogleSignIn() {
        // UI will trigger rememberGoogleAuthClient
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    fun onAppleSignIn() {
        // TODO: Trigger native Apple Sign-In → send idToken to backend
        _uiState.update { it.copy(errorMessage = Res.string.error_apple_unavailable) }
    }

    fun clearError() {
        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
    }

    fun resetLoginSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }

    // ── Two Factor Auth ────────────────────────────────────────

    fun onTwoFactorCodeChanged(code: String) {
        _uiState.update { it.copy(twoFactorCode = code, errorMessage = null) }
    }

    fun onVerifyTwoFactorClicked() {
        val state = _uiState.value
        val userId = state.requiresTwoFactorUserId ?: return
        if (state.twoFactorCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = Res.string.error_missing_fields) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.verifyTwoFactor(userId, state.twoFactorCode)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true, requiresTwoFactorUserId = null) }
                }
                is AuthResult.RequiresTwoFactor -> {
                    // Should not happen during verification
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = Res.string.error_register_failed, // TODO: Use generic error or specific 2FA error
                        )
                    }
                }
            }
        }
    }
}

/**
 * UI state for authentication screens.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val userName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: StringResource? = null,
    val loginSuccess: Boolean = false,
    val requiresTwoFactorUserId: String? = null,
    val twoFactorCode: String = "",
)

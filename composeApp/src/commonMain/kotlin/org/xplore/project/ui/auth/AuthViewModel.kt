package org.xplore.project.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xplore.project.domain.repository.AuthRepository

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
            _uiState.update { it.copy(errorMessage = "Inserisci email e password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(state.email, state.password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                },
                onFailure = {
                    // Generic error message — never reveal if email exists or password is wrong
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Credenziali non valide. Riprova.",
                        )
                    }
                },
            )
        }
    }

    // ── Guest ────────────────────────────────────────────────

    fun onGuestAccess() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.guestLogin()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Impossibile accedere. Controlla la connessione.",
                        )
                    }
                },
            )
        }
    }

    // ── Register ─────────────────────────────────────────────

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name, errorMessage = null) }
    }

    fun onRegisterClicked() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Compila tutti i campi") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.register(state.email, state.password, state.displayName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Registrazione fallita. Riprova.",
                        )
                    }
                },
            )
        }
    }

    // ── Social Login (Prepared for backend) ──────────────────

    fun onGoogleSignIn() {
        // TODO: Trigger native Google Sign-In → send idToken to backend
        _uiState.update { it.copy(errorMessage = "Google Sign-In non ancora disponibile") }
    }

    fun onAppleSignIn() {
        // TODO: Trigger native Apple Sign-In → send idToken to backend
        _uiState.update { it.copy(errorMessage = "Apple Sign-In non ancora disponibile") }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetLoginSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }
}

/**
 * UI state for authentication screens.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
)

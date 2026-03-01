package org.xplore.project.ui.profile

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
import org.xplore.project.data.local.TokenManager
import xploreapp.composeapp.generated.resources.*

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: StringResource? = null,
    val isSelectingTwoFactorMethod: Boolean = false,
    val selectedTwoFactorMethod: String? = null,
    val setupTwoFactorKey: String? = null,
    val setupTwoFactorUri: String? = null,
    val isTwoFactorEnabled: Boolean = false, // True when successfully setup
    val twoFactorCodeInput: String = "",
    val isGuest: Boolean = false,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isGuest = tokenManager.isGuest))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onInitiateTwoFactorSetup() {
        _uiState.update { it.copy(isSelectingTwoFactorMethod = true, errorMessage = null) }
    }

    fun onTwoFactorMethodSelected(method: String) {
        _uiState.update { it.copy(isSelectingTwoFactorMethod = false, selectedTwoFactorMethod = method, errorMessage = null) }
        when (method) {
            "Authenticator" -> startAuthenticatorSetup()
            "Email" -> startEmailSetup()
        }
    }

    fun onCancelTwoFactorSetup() {
        _uiState.update { it.copy(
            isSelectingTwoFactorMethod = false,
            setupTwoFactorKey = null,
            setupTwoFactorUri = null,
            selectedTwoFactorMethod = null,
            errorMessage = null,
            twoFactorCodeInput = ""
        ) }
    }

    private fun startAuthenticatorSetup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.setupTwoFactor()
            result.fold(
                onSuccess = { pair ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            setupTwoFactorKey = pair.first,
                            setupTwoFactorUri = pair.second 
                        ) 
                    }
                },
                onFailure = { e ->
                    // For now, reuse a generic error or create a specific one
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = Res.string.error_register_failed) 
                    }
                }
            )
        }
    }

    private fun startEmailSetup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userResult = authRepository.getCurrentUser()
            userResult.fold(
                onSuccess = { userInfo ->
                    val sendResult = authRepository.sendEmail2Fa(userInfo.id)
                    sendResult.fold(
                        onSuccess = {
                            _uiState.update { it.copy(isLoading = false, setupTwoFactorKey = "EmailSent") } // Hack to trigger dialog
                        },
                        onFailure = {
                            _uiState.update { it.copy(isLoading = false, errorMessage = Res.string.error_register_failed) }
                        }
                    )
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, errorMessage = Res.string.error_guest_failed) }
                }
            )
        }
    }

    fun onTwoFactorCodeChanged(code: String) {
        _uiState.update { it.copy(twoFactorCodeInput = code, errorMessage = null) }
    }

    fun onVerifySetupClicked() {
        val state = _uiState.value
        if (state.twoFactorCodeInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = Res.string.error_missing_fields) }
            return
        }

        val provider = state.selectedTwoFactorMethod ?: "Authenticator"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val userResult = authRepository.getCurrentUser()
            userResult.fold(
                onSuccess = { userInfo ->
                    val cleanCode = state.twoFactorCodeInput.trim().replace(" ", "")
                    val verifyResult = authRepository.verifyTwoFactor(userInfo.id, cleanCode, provider)
                    when (verifyResult) {
                        is AuthResult.Success -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    isTwoFactorEnabled = true,
                                    setupTwoFactorKey = null,
                                    setupTwoFactorUri = null,
                                    twoFactorCodeInput = ""
                                ) 
                            }
                        }
                        else -> {
                            _uiState.update { 
                                it.copy(isLoading = false, errorMessage = Res.string.error_invalid_credentials) 
                            }
                        }
                    }
                },
                onFailure = {
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = Res.string.error_guest_failed) 
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

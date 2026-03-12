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

// ── Explorer Level definitions ──────────────────────────────────
data class ExplorerLevel(
    val level: Int,
    val nameRes: StringResource,
    val minPoints: Int,
)

val explorerLevels = listOf(
    ExplorerLevel(1, Res.string.level_1, 0),
    ExplorerLevel(2, Res.string.level_2, 50),
    ExplorerLevel(3, Res.string.level_3, 150),
    ExplorerLevel(4, Res.string.level_4, 300),
    ExplorerLevel(5, Res.string.level_5, 500),
    ExplorerLevel(6, Res.string.level_6, 800),
    ExplorerLevel(7, Res.string.level_7, 1200),
    ExplorerLevel(8, Res.string.level_8, 1800),
    ExplorerLevel(9, Res.string.level_9, 2600),
    ExplorerLevel(10, Res.string.level_10, 3500),
)

fun levelForPoints(points: Int): ExplorerLevel =
    explorerLevels.lastOrNull { points >= it.minPoints } ?: explorerLevels.first()

fun nextLevelThreshold(points: Int): Int {
    val next = explorerLevels.firstOrNull { it.minPoints > points }
    return next?.minPoints ?: explorerLevels.last().minPoints
}

// ── Simple friend model ─────────────────────────────────────────
data class Friend(
    val id: String,
    val displayName: String,
)

// ── UI State ────────────────────────────────────────────────────
data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: StringResource? = null,

    // User info
    val displayName: String = "",
    val email: String = "",
    val isGuest: Boolean = false,

    // Avatar
    val avatarInitial: String = "?",
    val avatarUrl: String? = null,
    val isAvatarDialogOpen: Boolean = false,
    val isAvatarUploading: Boolean = false,
    val isFullScreenAvatarOpen: Boolean = false,

    // Level
    val totalPoints: Int = 0,
    val currentLevel: ExplorerLevel = explorerLevels.first(),
    val nextLevelPoints: Int = explorerLevels[1].minPoints,

    // Friends
    val friends: List<Friend> = emptyList(),
    val isFriendsDialogOpen: Boolean = false,
    val friendSearchQuery: String = "",

    // 2FA
    val isSelectingTwoFactorMethod: Boolean = false,
    val selectedTwoFactorMethod: String? = null,
    val setupTwoFactorKey: String? = null,
    val setupTwoFactorUri: String? = null,
    val isTwoFactorEnabled: Boolean = false,
    val twoFactorCodeInput: String = "",
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isGuest = tokenManager.isGuest))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    // ── Profile loading ─────────────────────────────────────────
    private fun loadUserProfile() {
        if (tokenManager.isGuest) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.getCurrentUser()
            result.fold(
                onSuccess = { user ->
                    val name = user.displayName ?: user.email
                    val initial = name.firstOrNull()?.uppercase() ?: "?"

                    // Points: use a placeholder value – integrate real data when backend supports it
                    val points = 0
                    val level = levelForPoints(points)
                    val nextThreshold = nextLevelThreshold(points)

                    val newAvatarUrl = if (user.hasAvatar) {
                        "${authRepository.getAvatarUrl(user.id)}?v=${kotlin.random.Random.nextInt()}"
                    } else null

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            displayName = name,
                            email = user.email,
                            avatarInitial = initial,
                            avatarUrl = newAvatarUrl,
                            totalPoints = points,
                            currentLevel = level,
                            nextLevelPoints = nextThreshold,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                },
            )
        }
    }

    // ── Avatar ──────────────────────────────────────────────────
    fun openAvatarDialog() {
        _uiState.update { it.copy(isAvatarDialogOpen = true) }
    }

    fun closeAvatarDialog() {
        _uiState.update { it.copy(isAvatarDialogOpen = false) }
    }

    fun openFullScreenAvatar() {
        _uiState.update { it.copy(isFullScreenAvatarOpen = true) }
    }

    fun closeFullScreenAvatar() {
        _uiState.update { it.copy(isFullScreenAvatarOpen = false) }
    }

    fun uploadAvatar(imageBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAvatarUploading = true) }
            val result = authRepository.uploadAvatar(imageBytes, fileName)
            result.fold(
                onSuccess = { user ->
                    val newAvatarUrl = if (user.hasAvatar) {
                        "${authRepository.getAvatarUrl(user.id)}?v=${kotlin.random.Random.nextInt()}"
                    } else null
                    println("Avatar upload SUCCESS, avatarUrl=$newAvatarUrl")
                    _uiState.update {
                        it.copy(
                            isAvatarUploading = false,
                            isAvatarDialogOpen = false,
                            avatarUrl = newAvatarUrl,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isAvatarUploading = false,
                            errorMessage = Res.string.profile_avatar_error,
                        )
                    }
                },
            )
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAvatarUploading = true) }
            val result = authRepository.deleteAvatar()
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isAvatarUploading = false,
                            isAvatarDialogOpen = false,
                            avatarUrl = null,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isAvatarUploading = false,
                            errorMessage = Res.string.profile_avatar_error,
                        )
                    }
                },
            )
        }
    }

    // ── Friends ─────────────────────────────────────────────────
    fun openFriendsDialog() {
        _uiState.update { it.copy(isFriendsDialogOpen = true, friendSearchQuery = "") }
    }

    fun closeFriendsDialog() {
        _uiState.update { it.copy(isFriendsDialogOpen = false, friendSearchQuery = "") }
    }

    fun onFriendSearchQueryChanged(query: String) {
        _uiState.update { it.copy(friendSearchQuery = query) }
    }

    fun addFriend() {
        val query = _uiState.value.friendSearchQuery.trim()
        if (query.isBlank()) return
        // Placeholder: in a real app this would call an API
        val newFriend = Friend(id = query, displayName = query)
        _uiState.update {
            it.copy(
                friends = it.friends + newFriend,
                friendSearchQuery = "",
            )
        }
    }

    fun removeFriend(friendId: String) {
        _uiState.update {
            it.copy(friends = it.friends.filter { f -> f.id != friendId })
        }
    }

    // ── 2FA ─────────────────────────────────────────────────────
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
                onFailure = {
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
                            _uiState.update { it.copy(isLoading = false, setupTwoFactorKey = "EmailSent") }
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

    fun disableTwoFactor() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // TODO: call authRepository.disableTwoFactor() when backend endpoint is available
            // For now, just toggle the flag locally
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isTwoFactorEnabled = false,
                )
            }
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

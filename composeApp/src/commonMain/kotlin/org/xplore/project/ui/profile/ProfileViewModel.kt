package org.xplore.project.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.xplore.project.domain.model.GroupInvite
import org.xplore.project.domain.repository.AuthRepository
import org.xplore.project.domain.repository.AuthResult
import org.xplore.project.domain.repository.CommunityRepository
import org.xplore.project.domain.repository.FriendRepository
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

// ── Friend models ───────────────────────────────────────────────
data class Friend(
    val id: String,
    val displayName: String?,
    val hasAvatar: Boolean = false,
)

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromDisplayName: String?,
    val fromHasAvatar: Boolean = false,
    val sentAt: String = "",
)

data class SearchUser(
    val userId: String,
    val displayName: String?,
    val hasAvatar: Boolean = false,
    val friendshipStatus: String? = null,
)

// ── UI State ────────────────────────────────────────────────────
data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: StringResource? = null,

    // User info
    val currentUserId: String = "",
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
    val friendsSelectedTab: Int = 0,
    val sentRequests: List<FriendRequest> = emptyList(),
    val receivedRequests: List<FriendRequest> = emptyList(),
    val searchResults: List<SearchUser> = emptyList(),
    val isSearchingFriends: Boolean = false,

    // Group Invites
    val groupInvites: List<GroupInvite> = emptyList(),
    val isGroupInvitesDialogOpen: Boolean = false,
    val isLoadingInvites: Boolean = false,

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
    private val friendRepository: FriendRepository,
    private val communityRepository: CommunityRepository,
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

                    val points = 0
                    val level = levelForPoints(points)
                    val nextThreshold = nextLevelThreshold(points)

                    val newAvatarUrl = if (user.hasAvatar) {
                        "${authRepository.getAvatarUrl(user.id)}?v=${kotlin.random.Random.nextInt()}"
                    } else null

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUserId = user.id,
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
            loadFriendsData()
        }
    }

    private suspend fun loadFriendsData() {
        val friendsResult = friendRepository.getFriends()
        val requestsResult = friendRepository.getPendingRequests()

        val friends = friendsResult.getOrNull()?.map {
            Friend(id = it.userId, displayName = it.displayName, hasAvatar = it.hasAvatar)
        } ?: return

        val received = requestsResult.getOrNull()?.map {
            FriendRequest(
                id = it.requestId,
                fromUserId = it.requesterId,
                fromDisplayName = it.requesterDisplayName,
                fromHasAvatar = it.requesterHasAvatar,
                sentAt = it.sentAt,
            )
        } ?: emptyList()

        _uiState.update { it.copy(friends = friends, receivedRequests = received) }
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
        _uiState.update {
            it.copy(
                isFriendsDialogOpen = true,
                friendSearchQuery = "",
                searchResults = emptyList(),
                friendsSelectedTab = 0,
            )
        }
    }

    fun closeFriendsDialog() {
        _uiState.update {
            it.copy(
                isFriendsDialogOpen = false,
                friendSearchQuery = "",
                searchResults = emptyList(),
            )
        }
    }

    fun onFriendsTabSelected(index: Int) {
        _uiState.update { it.copy(friendsSelectedTab = index) }
        if (index == 0) {
            viewModelScope.launch {
                val requestsResult = friendRepository.getPendingRequests()
                requestsResult.getOrNull()?.let { dtos ->
                    val received = dtos.map {
                        FriendRequest(
                            id = it.requestId,
                            fromUserId = it.requesterId,
                            fromDisplayName = it.requesterDisplayName,
                            fromHasAvatar = it.requesterHasAvatar,
                            sentAt = it.sentAt,
                        )
                    }
                    _uiState.update { it.copy(receivedRequests = received) }
                }
            }
        }
    }

    fun onFriendSearchQueryChanged(query: String) {
        _uiState.update { it.copy(friendSearchQuery = query) }
    }

    fun searchUsers() {
        val query = _uiState.value.friendSearchQuery.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingFriends = true) }
            val result = friendRepository.searchUsers(query)
            result.fold(
                onSuccess = { dtos ->
                    val results = dtos.map {
                        SearchUser(
                            userId = it.userId,
                            displayName = it.displayName,
                            hasAvatar = it.hasAvatar,
                            friendshipStatus = it.friendshipStatus,
                        )
                    }
                    _uiState.update { it.copy(isSearchingFriends = false, searchResults = results) }
                },
                onFailure = {
                    _uiState.update { it.copy(isSearchingFriends = false, searchResults = emptyList()) }
                },
            )
        }
    }

    fun sendFriendRequest(addresseeId: String) {
        viewModelScope.launch {
            val result = friendRepository.sendFriendRequest(addresseeId)
            result.fold(
                onSuccess = {
                    val query = _uiState.value.friendSearchQuery.trim()
                    if (query.isNotBlank()) searchUsers()
                },
                onFailure = { /* silently ignore, status visible in search results */ },
            )
        }
    }

    fun cancelFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.rejectRequest(requestId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(sentRequests = it.sentRequests.filter { r -> r.id != requestId }) }
                },
                onFailure = { /* silently ignore */ },
            )
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.acceptRequest(requestId)
            result.fold(
                onSuccess = {
                    val friendsResult = friendRepository.getFriends()
                    val requestsResult = friendRepository.getPendingRequests()
                    val friends = friendsResult.getOrNull()?.map {
                        Friend(id = it.userId, displayName = it.displayName, hasAvatar = it.hasAvatar)
                    }
                    val received = requestsResult.getOrNull()?.map {
                        FriendRequest(
                            id = it.requestId,
                            fromUserId = it.requesterId,
                            fromDisplayName = it.requesterDisplayName,
                            fromHasAvatar = it.requesterHasAvatar,
                            sentAt = it.sentAt,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            friends = friends ?: it.friends,
                            receivedRequests = received ?: it.receivedRequests,
                        )
                    }
                },
                onFailure = { /* silently ignore */ },
            )
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.rejectRequest(requestId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(receivedRequests = it.receivedRequests.filter { r -> r.id != requestId }) }
                },
                onFailure = { /* silently ignore */ },
            )
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            val result = friendRepository.removeFriend(friendId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(friends = it.friends.filter { f -> f.id != friendId }) }
                },
                onFailure = { /* silently ignore */ },
            )
        }
    }

    // ── Group Invites ─────────────────────────────────────────
    fun openGroupInvitesDialog() {
        _uiState.update { it.copy(isGroupInvitesDialogOpen = true) }
        loadGroupInvites()
    }

    fun closeGroupInvitesDialog() {
        _uiState.update { it.copy(isGroupInvitesDialogOpen = false) }
    }

    private fun loadGroupInvites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInvites = true) }
            try {
                val invites = communityRepository.getMyGroupInvites()
                _uiState.update { it.copy(isLoadingInvites = false, groupInvites = invites) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingInvites = false) }
            }
        }
    }

    fun acceptGroupInvite(inviteId: String) {
        viewModelScope.launch {
            try {
                communityRepository.acceptGroupInvite(inviteId)
                _uiState.update {
                    it.copy(groupInvites = it.groupInvites.filter { inv -> inv.id != inviteId })
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = Res.string.group_invite_error) }
            }
        }
    }

    fun rejectGroupInvite(inviteId: String) {
        viewModelScope.launch {
            try {
                communityRepository.rejectGroupInvite(inviteId)
                _uiState.update {
                    it.copy(groupInvites = it.groupInvites.filter { inv -> inv.id != inviteId })
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = Res.string.group_invite_error) }
            }
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

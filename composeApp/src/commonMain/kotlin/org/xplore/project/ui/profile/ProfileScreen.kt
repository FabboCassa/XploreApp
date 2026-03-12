package org.xplore.project.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.ui.util.rememberImagePicker
import org.xplore.project.ui.util.rememberCameraPicker
import xploreapp.composeapp.generated.resources.*

/**
 * Profile detail screen with avatar, level, friends, 2FA & logout.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onClearMapCache: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Image picker — delivers bytes to the ViewModel
    val imagePicker = rememberImagePicker { picked ->
        viewModel.uploadAvatar(picked.bytes, picked.fileName)
    }

    // Camera picker
    val cameraPicker = rememberCameraPicker { picked ->
        viewModel.uploadAvatar(picked.bytes, picked.fileName)
    }

    if (uiState.isLoading && uiState.displayName.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Avatar Section ──
        ProfileHeader(
            initial = uiState.avatarInitial,
            displayName = if (uiState.isGuest) stringResource(Res.string.profile_guest_label) else uiState.displayName,
            email = if (uiState.isGuest) "" else uiState.email,
            avatarUrl = uiState.avatarUrl,
            onAvatarClick = {
                if (uiState.avatarUrl != null) {
                    viewModel.openFullScreenAvatar()
                } else {
                    viewModel.openAvatarDialog()
                }
            },
            onEditAvatar = viewModel::openAvatarDialog,
        )

        // ── Level Section ── (hide for guests)
        if (!uiState.isGuest) {
            LevelCard(
                level = uiState.currentLevel,
                currentPoints = uiState.totalPoints,
                nextLevelPoints = uiState.nextLevelPoints,
            )
        }

        // ── Friends Section ── (hide for guests)
        if (!uiState.isGuest) {
            FriendsCard(
                friendCount = uiState.friends.size,
                onClick = viewModel::openFriendsDialog,
            )
        }

        // ── Security Section ──
        if (!uiState.isGuest) {
            SecurityCard(
                isTwoFactorEnabled = uiState.isTwoFactorEnabled,
                isLoading = uiState.isLoading,
                onEnable2FA = viewModel::onInitiateTwoFactorSetup,
                onDisable2FA = viewModel::disableTwoFactor,
            )
        }

        // ── Settings Section ──
        SettingsCard(onClearMapCache = onClearMapCache)

        // ── Logout / Login Button ──
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isGuest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                contentColor = if (uiState.isGuest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(
                text = if (uiState.isGuest) stringResource(Res.string.profile_btn_login) else stringResource(Res.string.profile_btn_logout),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    // ── Avatar Edit Dialog ──
    if (uiState.isAvatarDialogOpen) {
        AvatarEditDialog(
            avatarUrl = uiState.avatarUrl,
            hasAvatar = uiState.avatarUrl != null,
            isUploading = uiState.isAvatarUploading,
            onPickFromGallery = {
                imagePicker.launch()
            },
            onPickFromCamera = {
                cameraPicker.launch()
            },
            onRemove = viewModel::deleteAvatar,
            onDismiss = viewModel::closeAvatarDialog,
        )
    }

    // ── Friends Dialog ──
    if (uiState.isFriendsDialogOpen) {
        FriendsDialog(
            friends = uiState.friends,
            searchQuery = uiState.friendSearchQuery,
            onSearchChanged = viewModel::onFriendSearchQueryChanged,
            onAdd = viewModel::addFriend,
            onRemove = viewModel::removeFriend,
            onDismiss = viewModel::closeFriendsDialog,
        )
    }

    // ── 2FA Method Selection Dialog ──
    if (uiState.isSelectingTwoFactorMethod) {
        TwoFactorMethodDialog(viewModel)
    }

    // ── 2FA Setup Dialog ──
    if (uiState.setupTwoFactorKey != null) {
        TwoFactorSetupDialog(uiState, viewModel)
    }

    // ── Full Screen Avatar Dialog ──
    val url = uiState.avatarUrl
    if (uiState.isFullScreenAvatarOpen && url != null) {
        FullScreenAvatarDialog(
            avatarUrl = url,
            onDismiss = viewModel::closeFullScreenAvatar
        )
    }
}

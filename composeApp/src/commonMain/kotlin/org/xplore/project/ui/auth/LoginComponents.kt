package org.xplore.project.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.domain.auth.AppleAuthClient
import org.xplore.project.domain.auth.AppleSignInResult
import org.xplore.project.domain.auth.GoogleAuthClient
import org.xplore.project.domain.auth.GoogleSignInResult
import xploreapp.composeapp.generated.resources.*

@Composable
fun SocialLoginSection(
    viewModel: AuthViewModel,
    coroutineScope: CoroutineScope,
    googleAuthClient: GoogleAuthClient,
    appleAuthClient: AppleAuthClient
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colorScheme.outline.copy(alpha = 0.5f),
        )
        Text(
            text = stringResource(Res.string.login_or),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onBackground.copy(alpha = 0.5f),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colorScheme.outline.copy(alpha = 0.5f),
        )
    }

    Spacer(Modifier.height(24.dp))

    // Google Sign-In
    OutlinedButton(
        onClick = {
            viewModel.onGoogleSignIn() // Show loading state early
            coroutineScope.launch {
                when (val result = googleAuthClient.signIn()) {
                    is GoogleSignInResult.Success -> {
                        viewModel.onExternalLoginSuccess("Google", result.idToken)
                    }
                    is GoogleSignInResult.Error -> {
                        viewModel.onExternalLoginError("Google", result.message)
                    }
                    GoogleSignInResult.Cancelled -> {
                        viewModel.clearError()
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(stringResource(Res.string.login_google_btn), style = MaterialTheme.typography.labelLarge)
    }

    Spacer(Modifier.height(12.dp))

    // Apple Sign-In
    OutlinedButton(
        onClick = {
            viewModel.onAppleSignIn() // Show loading state early
            coroutineScope.launch {
                when (val result = appleAuthClient.signIn()) {
                    is AppleSignInResult.Success -> {
                        viewModel.onExternalLoginSuccess("Apple", result.identityToken)
                    }
                    is AppleSignInResult.Error -> {
                        viewModel.onExternalLoginError("Apple", result.message)
                    }
                    AppleSignInResult.Cancelled -> {
                        viewModel.clearError()
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(stringResource(Res.string.login_apple_btn), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun RegisterLink(
    onNavigateToRegister: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.login_no_account),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Text(
            text = stringResource(Res.string.login_register_link),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.primary,
            modifier = Modifier.clickable { onNavigateToRegister() },
        )
    }
}

@Composable
fun TwoFactorDialog(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    focusManager: FocusManager
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = viewModel::onCancelTwoFactor,
        title = { Text(text = stringResource(Res.string.two_factor_title)) },
        text = {
            Column {
                Text(text = stringResource(Res.string.two_factor_desc))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.twoFactorCode,
                    onValueChange = viewModel::onTwoFactorCodeChanged,
                    label = { Text(stringResource(Res.string.two_factor_code_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.onVerifyTwoFactorClicked()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = viewModel::onSendEmail2FaClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.two_factor_btn_send_email))
                }
                uiState.emailSentMessage?.let { msg ->
                    Text(
                        text = stringResource(msg),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp).align(Alignment.CenterHorizontally)
                    )
                }
                uiState.errorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(error),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::onVerifyTwoFactorClicked,
                enabled = uiState.twoFactorCode.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.two_factor_btn_verify))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onCancelTwoFactor) {
                Text(stringResource(Res.string.two_factor_btn_cancel))
            }
        }
    )
}

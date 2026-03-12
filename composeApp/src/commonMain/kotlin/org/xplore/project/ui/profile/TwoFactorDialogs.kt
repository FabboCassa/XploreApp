package org.xplore.project.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.*

@Composable
fun TwoFactorMethodDialog(viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::onCancelTwoFactorSetup,
        title = { Text(stringResource(Res.string.profile_2fa_choose_method_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.profile_2fa_choose_method_desc))
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.onTwoFactorMethodSelected("Authenticator") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(Res.string.profile_2fa_method_app))
                }
                OutlinedButton(
                    onClick = { viewModel.onTwoFactorMethodSelected("Email") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(Res.string.profile_2fa_method_email))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = viewModel::onCancelTwoFactorSetup) {
                Text(stringResource(Res.string.two_factor_btn_cancel))
            }
        },
    )
}

@Composable
fun TwoFactorSetupDialog(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    AlertDialog(
        onDismissRequest = viewModel::onCancelTwoFactorSetup,
        title = { Text(stringResource(Res.string.profile_2fa_setup_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (uiState.selectedTwoFactorMethod == "Email") {
                    Text(stringResource(Res.string.profile_2fa_setup_email_desc))
                    Spacer(Modifier.height(16.dp))
                } else {
                    Text(stringResource(Res.string.profile_2fa_setup_app_step_1))
                    Spacer(Modifier.height(8.dp))
                    uiState.setupTwoFactorUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            Image(
                                painter = rememberQrCodePainter(data = uri),
                                contentDescription = stringResource(Res.string.profile_2fa_qr_content_desc),
                                modifier = Modifier.size(160.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(
                        text = stringResource(Res.string.profile_2fa_setup_app_manual),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.setupTwoFactorKey ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(Res.string.profile_2fa_setup_app_step_2))
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = uiState.twoFactorCodeInput,
                    onValueChange = viewModel::onTwoFactorCodeChanged,
                    label = { Text(stringResource(Res.string.profile_2fa_confirmed_code_label)) },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp)
                )
                uiState.errorMessage?.let { errorMsg ->
                    Text(
                        text = stringResource(errorMsg),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::onVerifySetupClicked,
                enabled = uiState.twoFactorCodeInput.isNotBlank() && !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(Res.string.profile_2fa_btn_verify_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onCancelTwoFactorSetup) {
                Text(stringResource(Res.string.two_factor_btn_cancel))
            }
        },
    )
}

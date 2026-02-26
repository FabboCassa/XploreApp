package org.xplore.project.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import xploreapp.composeapp.generated.resources.*

/**
 * User Profile screen with Logout and Cache Management.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onClearMapCache: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.profile_coming_soon),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )

            Spacer(Modifier.height(32.dp))

            // ── Clear Map Cache ──
            OutlinedButton(onClick = onClearMapCache) {
                Text(stringResource(Res.string.clear_map_cache))
            }
            Text(
                text = stringResource(Res.string.clear_map_cache_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            // ── 2FA Setup ──
            if (uiState.isTwoFactorEnabled) {
                Text(
                    text = "Autenticazione a Due Fattori abilitata \u2705",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                OutlinedButton(onClick = viewModel::onInitiateTwoFactorSetup, enabled = !uiState.isLoading) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text("Abilita 2FA")
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Logout ──
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(Res.string.profile_btn_logout))
            }
        }
    }

    // ── Method Selection Dialog ──
    if (uiState.isSelectingTwoFactorMethod) {
        AlertDialog(
            onDismissRequest = viewModel::onCancelTwoFactorSetup,
            title = { Text("Scegli un metodo 2FA") },
            text = {
                Column {
                    Text("Seleziona come vuoi ricevere i codici di verifica a due fattori:")
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.onTwoFactorMethodSelected("Authenticator") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("App Authenticator (Google, Authy, ecc.)")
                    }
                    OutlinedButton(
                        onClick = { viewModel.onTwoFactorMethodSelected("Email") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("Email")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::onCancelTwoFactorSetup) {
                    Text("Annulla")
                }
            }
        )
    }

    // ── 2FA Setup Dialog (Authenticator or Email) ──
    if (uiState.setupTwoFactorKey != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Configura 2FA") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (uiState.selectedTwoFactorMethod == "Email") {
                        Text("Abbiamo inviato un codice di conferma al tuo indirizzo email. Inseriscilo qui sotto per completare l'attivazione.")
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Text("1. Scansiona questo QR Code con la tua app Authenticator:")
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
                                    contentDescription = "QR Code per 2FA",
                                    modifier = Modifier.size(160.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        Text(
                            text = "Oppure inserisci manualmente questa chiave:",
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
                        Text("2. Inserisci il codice a 6 cifre generato dall'app:")
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = uiState.twoFactorCodeInput,
                        onValueChange = viewModel::onTwoFactorCodeChanged,
                        label = { Text("Codice confermato") },
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
                    enabled = uiState.twoFactorCodeInput.isNotBlank() && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Verifica e Salva")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelTwoFactorSetup) {
                    Text("Annulla")
                }
            }
        )
    }
}

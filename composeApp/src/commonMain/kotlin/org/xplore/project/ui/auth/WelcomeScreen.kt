package org.xplore.project.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Welcome gate screen — first screen the user sees.
 *
 * Offers two paths:
 * - **"Accedi"**: Navigate to the Login screen.
 * - **"Continua senza account"**: Guest access with limited features.
 */
@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onGuestAccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    // If guest login succeeded, navigate
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            viewModel.resetLoginSuccess()
            onGuestAccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.background,
                        colorScheme.primaryContainer.copy(alpha = 0.3f),
                        colorScheme.background,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, 2000f),
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Logo / Title ──
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { -40 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Xplore",
                        style = MaterialTheme.typography.displayLarge,
                        color = colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Scopri la cultura intorno a te",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(64.dp))

            // ── Primary CTA: Login ──
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = "Accedi",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Secondary CTA: Guest ──
            OutlinedButton(
                onClick = { viewModel.onGuestAccess() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !uiState.isLoading,
            ) {
                Text(
                    text = if (uiState.isLoading) "Accesso in corso…" else "Continua senza account",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            // ── Error ──
            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

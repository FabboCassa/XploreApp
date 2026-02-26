package org.xplore.project.domain.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption

import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.google_web_client_id

class AndroidGoogleAuthClient(
    private val context: Context,
    private val webClientId: String
) : GoogleAuthClient {

    override suspend fun signIn(): GoogleSignInResult {
        return try {
            val credentialManager = CredentialManager.create(context)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId) 
                .setAutoSelectEnabled(false)
                .build()
                
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()
                
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .addCredentialOption(signInWithGoogleOption)
                .build()
                
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL || 
                credential is androidx.credentials.CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleIdTokenCredential.idToken)
            } else {
                GoogleSignInResult.Error("Tipo di credenziale non supportato: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: Exception) {
            GoogleSignInResult.Error(e.message ?: "Errore sconosciuto durante il login con Google")
        }
    }
}

@Composable
actual fun rememberGoogleAuthClient(): GoogleAuthClient {
    val context = LocalContext.current
    val webClientId = stringResource(Res.string.google_web_client_id)
    return remember(context, webClientId) { AndroidGoogleAuthClient(context, webClientId) }
}

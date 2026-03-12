package org.xplore.project.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.*

@Composable
fun AvatarEditDialog(
    hasAvatar: Boolean,
    isUploading: Boolean,
    onPickFromGallery: () -> Unit,
    onPickFromCamera: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text(stringResource(Res.string.profile_avatar_change)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(vertical = 8.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(Res.string.profile_avatar_uploading))
                } else {
                    // Gallery option
                    OutlinedButton(
                        onClick = onPickFromGallery,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(Res.string.profile_avatar_from_gallery))
                    }

                    Spacer(Modifier.height(8.dp))

                    // Camera option
                    OutlinedButton(
                        onClick = onPickFromCamera,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(Res.string.profile_avatar_from_camera))
                    }

                    // Remove option (only shown if user has an avatar)
                    if (hasAvatar) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onRemove,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(stringResource(Res.string.profile_avatar_remove))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!isUploading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.close))
                }
            }
        },
    )
}

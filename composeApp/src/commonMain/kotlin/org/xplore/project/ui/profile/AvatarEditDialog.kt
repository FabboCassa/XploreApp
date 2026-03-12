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
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

@Composable
fun AvatarEditDialog(
    avatarUrl: String?,
    hasAvatar: Boolean,
    isUploading: Boolean,
    onPickFromGallery: () -> Unit,
    onPickFromCamera: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.profile_avatar_change),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(vertical = 8.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(Res.string.profile_avatar_uploading))
                } else {
                    // Preview existing avatar if available
                    if (avatarUrl != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar Preview",
                                modifier = Modifier.size(80.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

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

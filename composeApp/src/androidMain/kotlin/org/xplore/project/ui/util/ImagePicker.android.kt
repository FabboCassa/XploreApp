package org.xplore.project.ui.util

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    onImagePicked(PickedImage(bytes = bytes, fileName = "avatar.jpg"))
                }
            } catch (_: Exception) {
                // silently fail – error is handled at ViewModel level
            }
        }
    }
    return remember(launcher) {
        ImagePickerLauncher(
            launch = {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }
}

actual class ImagePickerLauncher(
    private val launch: () -> Unit,
) {
    actual fun launch() = launch.invoke()
}

@Composable
actual fun rememberCameraPicker(
    onImagePicked: (PickedImage) -> Unit,
): CameraPickerLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
                val bytes = stream.toByteArray()
                onImagePicked(PickedImage(bytes = bytes, fileName = "camera_avatar.jpg"))
            } catch (_: Exception) {
                // silently fail
            }
        }
    }
    return remember(launcher) {
        CameraPickerLauncher(
            launch = { launcher.launch(null) }
        )
    }
}

actual class CameraPickerLauncher(
    private val launch: () -> Unit,
) {
    actual fun launch() = launch.invoke()
}

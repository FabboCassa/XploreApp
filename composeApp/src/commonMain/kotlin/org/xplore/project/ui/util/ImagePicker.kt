package org.xplore.project.ui.util

import androidx.compose.runtime.Composable

/**
 * Data class holding the picked image bytes and file name.
 */
data class PickedImage(
    val bytes: ByteArray,
    val fileName: String,
)

/**
 * Platform-specific image picker.
 * Returns a launcher that, when invoked via launch(), opens the
 * gallery and delivers a [PickedImage] through [onImagePicked].
 */
@Composable
expect fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
): ImagePickerLauncher

/**
 * Launcher abstraction to trigger the image picker.
 */
expect class ImagePickerLauncher {
    fun launch()
}

/**
 * Platform-specific camera picker.
 * Returns a launcher that, when invoked via launch(), opens the
 * camera and delivers a [PickedImage] through [onImagePicked].
 */
@Composable
expect fun rememberCameraPicker(
    onImagePicked: (PickedImage) -> Unit,
): CameraPickerLauncher

/**
 * Launcher abstraction to trigger the camera picker.
 */
expect class CameraPickerLauncher {
    fun launch()
}

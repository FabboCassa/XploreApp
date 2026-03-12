@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.xplore.project.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSItemProvider
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
actual fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher(onImagePicked)
    }
}

actual class ImagePickerLauncher(
    private val onImagePicked: (PickedImage) -> Unit,
) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun launch() {
        val config = PHPickerConfiguration()
        config.filter = PHPickerFilter.imagesFilter
        config.selectionLimit = 1

        val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)
                val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                val provider: NSItemProvider = result.itemProvider

                if (provider.hasItemConformingToTypeIdentifier("public.image")) {
                    provider.loadDataRepresentationForTypeIdentifier("public.image") { data, error ->
                        if (error == null && data != null) {
                            val bytes = ByteArray(data.length.toInt())
                            bytes.usePinned {
                                platform.posix.memcpy(it.addressOf(0), data.bytes, data.length)
                            }
                            MainScope().launch {
                                onImagePicked(PickedImage(bytes = bytes, fileName = "avatar.jpg"))
                            }
                        }
                    }
                }
            }
        }

        val picker = PHPickerViewController(configuration = config)
        picker.delegate = delegate
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberCameraPicker(
    onImagePicked: (PickedImage) -> Unit,
): CameraPickerLauncher {
    return remember {
        CameraPickerLauncher(onImagePicked)
    }
}

actual class CameraPickerLauncher(
    private val onImagePicked: (PickedImage) -> Unit,
) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun launch() {
        if (!platform.UIKit.UIImagePickerController.isSourceTypeAvailable(
                platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            )
        ) {
            return
        }

        val picker = platform.UIKit.UIImagePickerController()
        picker.sourceType = platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.allowsEditing = true
        
        val delegate = object : NSObject(), platform.UIKit.UIImagePickerControllerDelegateProtocol, platform.UIKit.UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: platform.UIKit.UIImagePickerController,
                didFinishPickingImage: UIImage,
                editingInfo: Map<Any?, *>?
            ) {
                picker.dismissViewControllerAnimated(true, null)
                val data: NSData = UIImageJPEGRepresentation(didFinishPickingImage, 0.85) ?: return
                val bytes = ByteArray(data.length.toInt())
                bytes.usePinned {
                    platform.posix.memcpy(it.addressOf(0), data.bytes, data.length)
                }
                MainScope().launch {
                    onImagePicked(PickedImage(bytes = bytes, fileName = "camera_avatar.jpg"))
                }
            }

            override fun imagePickerControllerDidCancel(picker: platform.UIKit.UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
            }
        }

        picker.delegate = delegate
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}

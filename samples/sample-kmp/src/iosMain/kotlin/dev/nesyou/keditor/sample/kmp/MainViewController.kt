package dev.nesyou.keditor.sample.kmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import dev.nesyou.keditor.VideoEditor
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.resolve
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToURL
import platform.Photos.PHAsset
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject


fun MainViewController() = ComposeUIViewController {

    PHPhotoLibrary.requestAuthorization {

    }
    val editor = VideoEditor()

    Scaffold { padding ->
        val view = LocalUIViewController.current

        LaunchedEffect(Unit) {
            pickVideo(view) { path ->

                if (path != null) {
                   editor.test(path, FileKit.cacheDir.resolve("output.mp4").absolutePath())
                }
            }
        }
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Text(editor.ffmpegVersion())
            Text(editor.ffmpegMajorVersion().toString())
        }
    }

}


@OptIn(ExperimentalForeignApi::class)
fun pickVideo(
    viewController: UIViewController,
    onResult: (String?) -> Unit
) {
    val configuration = PHPickerConfiguration().apply {
        filter = PHPickerFilter.videosFilter
        selectionLimit = 1
    }

    val picker = PHPickerViewController(configuration)

    val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {

        override fun picker(
            picker: PHPickerViewController,
            didFinishPicking: List<*>
        ) {
            picker.dismissViewControllerAnimated(false) {

            }

            val result = didFinishPicking.firstOrNull()
                    as? PHPickerResult

            if (result == null) {
                onResult(null)
                return
            }

            val provider = result.itemProvider

            provider.loadFileRepresentationForTypeIdentifier(
                typeIdentifier = "public.movie"
            ) { url, error ->

                if (url == null || error != null) {
                    onResult(null)
                    return@loadFileRepresentationForTypeIdentifier
                }

                val fileName = "input_${NSUUID().UUIDString}.mp4"

                val destination = NSURL.fileURLWithPath(
                    NSTemporaryDirectory() + fileName
                )

                NSFileManager.defaultManager.copyItemAtURL(
                    srcURL = url,
                    toURL = destination,
                    error = null
                )

                onResult(destination.path)
            }
        }
    }

    picker.delegate = delegate

    viewController.presentViewController(
        picker,
        animated = true,
        completion = null
    )
}
@OptIn(ExperimentalForeignApi::class)
fun exportPhoto(
    asset: PHAsset,
    outputPath: String,
    completion: (String?) -> Unit
) {
    val options = PHImageRequestOptions()

    PHImageManager.defaultManager().requestImageDataAndOrientationForAsset(
        asset = asset,
        options = options
    ) { data, _, _, _ ->

        if (data == null) {
            completion(null)
            return@requestImageDataAndOrientationForAsset
        }

        val url = NSURL.fileURLWithPath(outputPath)

        if (data.writeToURL(url, atomically = true)) {
            completion(outputPath)
        } else {
            completion(null)
        }
    }
}
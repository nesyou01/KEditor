package dev.nesyou.keditor

import dev.nesyou.keditor.callbacks.ProgressCallback
import dev.nesyou.keditor.ffmpeg.AndroidMediaEditor

internal actual object PlatformKEditor {

    actual fun applyFilter(
        input: String,
        output: String,
        filters: String,
        progress: ProgressCallback
    ) {
        AndroidMediaEditor.applyFilter(
            inputPath = input,
            outputPath = output,
            filterDescr = filters,
            progress = progress::onProgress
        )
    }

}
package dev.nesyou.keditor

import dev.nesyou.keditor.callbacks.ProgressCallback
import keditor.apply_video_filter
import kotlinx.cinterop.ExperimentalForeignApi

internal actual object PlatformKEditor {

    @OptIn(ExperimentalForeignApi::class)
    actual fun applyFilter(
        input: String,
        output: String,
        filters: String,
        progress : ProgressCallback
    ) {
        apply_video_filter(input, output, filters)
    }

}
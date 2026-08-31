package dev.nesyou.keditor

import dev.nesyou.keditor.callbacks.ProgressCallback
import dev.nesyou.keditor.ffmpeg.AndroidMediaEditor
import kotlin.time.Duration

internal actual object PlatformKEditor {

    actual fun applyFilter(
        input: String,
        output: String,
        filters: String,
        start: Duration?,
        end: Duration?,
        removeAudio: Boolean,
        removeVideo: Boolean,
        crf: Int?,
        preset: String,
        progress: ProgressCallback
    ) {
        AndroidMediaEditor.applyFilter(
            inputPath = input,
            outputPath = output,
            filterDescr = filters,
            startMs = start?.inWholeMilliseconds,
            endMs = end?.inWholeMilliseconds,
            removeAudio = removeAudio,
            removeVideo = removeVideo,
            preset = preset,
            crf = crf,
            progress = progress::onProgress
        )
    }

}
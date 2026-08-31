package dev.nesyou.keditor.ffmpeg

object AndroidMediaEditor {

    init {
        System.loadLibrary("keditor")
    }

    private external fun nativeApplyFilter(
        inputPath: String,
        outputPath: String,
        filterDescr: String,
        start: Long,
        end: Long,
        removeAudio: Boolean,
        removeVideo: Boolean,
        crf: Int,
        preset: String,
        progress: (Float) -> Unit
    ): Int


    fun applyFilter(
        inputPath: String,
        outputPath: String,
        filterDescr: String,
        startMs: Long?,
        endMs: Long?,
        removeAudio: Boolean,
        removeVideo: Boolean,
        crf: Int?,
        preset: String,
        progress: (Float) -> Unit
    ) {
        nativeApplyFilter(
            inputPath = inputPath,
            outputPath = outputPath,
            filterDescr = filterDescr,
            progress = progress,
            start = startMs ?: -1L,
            end = endMs ?: -1L,
            removeAudio = removeAudio,
            removeVideo = removeVideo,
            crf = crf ?: -1,
            preset = preset
        )
    }
}
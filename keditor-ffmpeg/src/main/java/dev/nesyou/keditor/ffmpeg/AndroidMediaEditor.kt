package dev.nesyou.keditor.ffmpeg

object AndroidMediaEditor {

    init {
        System.loadLibrary("keditor")
    }

    private external fun nativeApplyFilter(
        inputPath: String,
        outputPath: String,
        filterDescr: String,
        progress: (Float) -> Unit
    ): Int


    fun applyFilter(
        inputPath: String,
        outputPath: String,
        filterDescr: String,
        progress: (Float) -> Unit
    ) {
        nativeApplyFilter(
            inputPath = inputPath,
            outputPath = outputPath,
            filterDescr = filterDescr,
            progress = progress
        )
    }
}
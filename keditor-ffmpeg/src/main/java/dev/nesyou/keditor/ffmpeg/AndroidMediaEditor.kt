package dev.nesyou.keditor.ffmpeg

object AndroidMediaEditor {

    init {
        System.loadLibrary("keditor")
    }

    private external fun nativeApplyFilter(
        inputPath: String,
        outputPath: String,
        filterDescr: String
    ): Int


    fun applyFilter(
        inputPath: String,
        outputPath: String,
        filterDescr: String
    ) {
        nativeApplyFilter(
            inputPath = inputPath,
            outputPath = outputPath,
            filterDescr = filterDescr
        )
    }
}
package dev.nesyou.keditor.ffmpeg

class VideoEditor {

    init {
        System.loadLibrary("keditor")
    }

    private external fun nativeApplyFilter(
        inputPath: String,
        outputPath: String,
        filterDescr: String
    ): Int


    fun ffmpegVersion(): String {
        return "asdasd"
    }

    fun testCropaa(
        inputPath: String,
        outputPath: String,
        filterDescr: String
    ): Int {
        return nativeApplyFilter(inputPath, outputPath, filterDescr)
    }
}
package dev.nesyou.keditor.ffmpeg

class VideoEditor {

    init {
        System.loadLibrary("keditor")
    }

    private external fun testCrop(
        inputPath: String,
        outputPath: String
    ): Int


    fun ffmpegVersion(): String {
        return "asdasd"
    }

    fun testCropaa(
        inputPath: String,
        outputPath: String
    ): Int {
        return testCrop(inputPath, outputPath)
    }
}
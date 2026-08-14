package dev.nesyou.keditor

class VideoEditor {

    init {
        System.loadLibrary("keditor")
    }

    private external fun nativeGetFFmpegVersion(): String

    private external fun nativeGetFFmpegMajorVersion(): Int

    fun ffmpegVersion(): String {
        return nativeGetFFmpegVersion()
    }

    fun ffmpegMajorVersion(): Int {
        return nativeGetFFmpegMajorVersion()
    }
}
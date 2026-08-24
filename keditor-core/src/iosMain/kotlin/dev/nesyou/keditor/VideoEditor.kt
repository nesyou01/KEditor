package dev.nesyou.keditor

import keditor.apply_video_filter
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class VideoEditor {

    fun test(path: String, out: String) {
        val result = apply_video_filter(path, out, "crop=640:480:0:0")
        println("UNESS $result $out")
    }

    fun ffmpegVersion(): String {
        return ""
    }

    fun ffmpegMajorVersion(): Int {
        return 1
    }
}
package dev.nesyou.keditor

import keditor.av_version_info
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
class VideoEditor {

    fun ffmpegVersion(): String {
        return av_version_info()?.toKString() ?: ""
    }

    fun ffmpegMajorVersion(): Int {
        return 1
    }
}
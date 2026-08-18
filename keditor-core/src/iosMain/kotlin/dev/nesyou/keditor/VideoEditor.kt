package dev.nesyou.keditor

import keditor.AVFormatContext
import keditor.av_version_info
import keditor.avcodec_find_encoder_by_name
import keditor.avformat_close_input
import keditor.avformat_open_input
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
class VideoEditor {

    fun test(path: String) {
        memScoped {
            val x264 = avcodec_find_encoder_by_name("libx264")

            if (x264 == null) {
                println("❌ libx264 NOT found")
            } else {
                println("✅ libx264 found!")

                println(
                    "Encoder: ${
                        x264.pointed.name?.toKString()
                    }"
                )

                println("Long name: ${x264.pointed.long_name?.toKString()}")

                println(
                    "ID: ${x264.pointed.id}"
                )
            }


        }
    }

    fun ffmpegVersion(): String {
        return av_version_info()?.toKString() ?: ""
    }

    fun ffmpegMajorVersion(): Int {
        return 1
    }
}
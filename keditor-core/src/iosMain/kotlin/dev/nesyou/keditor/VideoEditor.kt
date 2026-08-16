package dev.nesyou.keditor

import keditor.AVFormatContext
import keditor.av_version_info
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
            val context = alloc<CPointerVar<AVFormatContext>>()

            val result = avformat_open_input(
                context.ptr,
                path,
                null,
                null
            )

            println("avformat_open_input result = $result")

            if (result < 0) {
                println("Failed to open video")
                return
            }

            val formatContext = context.value!!

            println("Opened successfully!")
            println("Format: ${formatContext.pointed.iformat?.pointed?.name?.toKString()}")
            println("Streams: ${formatContext.pointed.nb_streams}")

            for (i in 0 until formatContext.pointed.nb_streams.toInt()) {
                val stream = formatContext.pointed.streams!![i]!!
                val codecPar = stream.pointed.codecpar!!

                println(
                    "Stream $i: " +
                            "codec_type=${codecPar.pointed.codec_type}, " +
                            "codec_id=${codecPar.pointed.codec_id}, " +
                            "width=${codecPar.pointed.width}, " +
                            "height=${codecPar.pointed.height}"
                )
            }

            avformat_close_input(context.ptr)
        }
    }

    fun ffmpegVersion(): String {
        return av_version_info()?.toKString() ?: ""
    }

    fun ffmpegMajorVersion(): Int {
        return 1
    }
}
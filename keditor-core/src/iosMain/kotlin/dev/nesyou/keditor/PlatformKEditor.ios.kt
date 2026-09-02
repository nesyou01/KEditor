package dev.nesyou.keditor

import dev.nesyou.keditor.callbacks.ProgressCallback
import keditor.apply_video_filter
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toByte
import kotlin.time.Duration

internal actual object PlatformKEditor {

    class ProgressContext(
        val callback: (Float) -> Unit
    )

    @OptIn(ExperimentalForeignApi::class)
    actual fun applyFilter(
        input: String,
        output: String,
        filters: String,
        start: Duration?,
        end: Duration?,
        removeAudio: Boolean,
        removeVideo: Boolean,
        crf: Int?,
        preset: String,
        progress: ProgressCallback
    ) {
        memScoped {
            val context = StableRef.create(
                ProgressContext(callback = progress::onProgress)
            )

            fun progressCallback(
                context: CPointer<out CPointed>?,
                progress: Float
            ) {
                if (context == null) return

                val stableRef = context.asStableRef<ProgressContext>()
                stableRef.get().callback(progress)
            }

            apply_video_filter(
                env = context.asCPointer(),
                in_filename = input,
                out_filename = output,
                filter_descr = filters,
                progress_callback = staticCFunction(::progressCallback),
                start = start?.inWholeMilliseconds ?: -1,
                end = end?.inWholeMilliseconds ?: -1,
                remove_video = removeVideo.toByte().toUByte(),
                remove_audio = removeAudio.toByte().toUByte(),
                crf = crf ?: -1,
                preset = preset
            )
        }
    }


}
package dev.nesyou.keditor

import dev.nesyou.keditor.callbacks.ProgressCallback
import dev.nesyou.keditor.configs.Speed
import dev.nesyou.keditor.configs.TrimConfig
import dev.nesyou.keditor.filters.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.time.Duration

//
// Created by Youness Lagmah on 8/24/26.
//


/**

 * Main entry point for processing and editing video files.
 *
 * [KEditor] provides a Kotlin API for common video editing operations such as:
 *
 * * Trimming videos
 * * Adding FFmpeg filters
 * * Changing playback speed
 * * Removing audio or video streams
 * * Compressing videos by configuring the CRF value
 * * Reporting processing progress
 *
 * Instances should be created using the [Config] DSL:
 *
 * ```
```
 * val editor = KEditor {
 * ```
trim(Duration.ZERO, 10.seconds)
```
 * ```
removeAudio()
```
 * ```
compress(quality = 70)
```
 * }
 * ```
```
 *
 * The actual video processing is delegated to [PlatformKEditor].
 */
class KEditor private constructor(
    /**

     * Configuration describing the section of the input video that should
     * be processed.
     */
    private val trimConfig: TrimConfig,

    /**

     * Collection of filters that will be applied to the video.
     */
    private val filters: Collection<Filter>,

    /**

     * Coroutine context used to execute the video processing operation.
     *
     * The default context is [Dispatchers.IO], since video processing is
     * an I/O- and CPU-intensive operation.
     */
    private val coroutineContext: CoroutineContext,

    /**

     * Whether the audio stream should be removed from the output.
     */
    private val removeAudio: Boolean,

    /**

     * Whether the video stream should be removed from the output.
     */
    private val removeVideo: Boolean,

    /**

     * Optional Constant Rate Factor (CRF) value used for video encoding.
     *
     * A lower CRF generally produces higher quality and a larger file,
     * while a higher CRF generally produces lower quality and a smaller file.
     */
    private val crf: Int?,

    /**

     * Encoding preset used by the underlying video encoder.
     *
     * The preset is derived from the configured [Speed].
     */
    private val preset: String
) {

    /**

     * Builder used to configure a [KEditor] instance.
     *
     * This class provides a DSL-style API for configuring video processing
     * before creating the final [KEditor] instance.
     */
    class Config {

        /**

         * Trim configuration.
         *
         * [TrimConfig.UNSET] means that no trimming is requested.
         */
        private var trimConfig: TrimConfig = TrimConfig.UNSET

        /**

         * Indicates whether audio should be removed.
         */
        private var removeAudio: Boolean = false

        /**

         * Indicates whether video should be removed.
         */
        private var removeVideo: Boolean = false

        /**

         * Controls the encoding speed/preset.
         *
         * Defaults to [Speed.Medium].
         */
        private var speed: Speed = Speed.Medium

        /**

         * Optional CRF value used for compression.
         *
         * `null` means that no explicit CRF value was configured.
         */
        private var crf: Int? = null

        /**

         * Coroutine context used when processing the video.
         *
         * Defaults to [Dispatchers.IO].
         */
        private var coroutineContext: CoroutineContext = Dispatchers.IO

        /**

         * Filters configured by the user.
         */
        private val filters = mutableListOf<Filter>()

        /**

         * Sets the trim configuration.
         */
        fun trim(config: TrimConfig) {
            this.trimConfig = config
        }

        /**

         * Removes the audio stream from the output video.
         */
        fun removeAudio() {
            this.removeAudio = true
        }

        /**

         * Enables the audio stream.
         *
         * Calling this after [removeAudio] cancels the audio removal.
         */
        fun enableAudio() {
            this.removeAudio = false
        }

        /**

         * Removes the video stream from the output.
         */
        fun removeVideo() {
            this.removeVideo = true
        }

        /**

         * Enables the video stream.
         *
         * Calling this after [removeVideo] cancels the video removal.
         */
        fun enableVideo() {
            this.removeVideo = false
        }

        /**

         * Trims the video between [start] and [end].
         */
        fun trim(start: Duration, end: Duration) {
            trim(
                TrimConfig(
                    start = start,
                    end = end
                )
            )
        }

        /**

         * Trims the video from the beginning up to [end].
         */
        fun trim(end: Duration) {
            trim(
                start = Duration.ZERO,
                end = end
            )
        }

        /**

         * Adds multiple [Filter] instances to the video.
         *
         * Filters are applied in the same order in which they are added.
         */
        fun filters(vararg filters: Filter) {
            this.filters += filters
        }

        /**

         * Adds a single [Filter] to the video.
         */
        fun filter(filter: Filter) {
            this.filters += filter
        }

        /**

         * Sets the encoding speed.
         *
         * The configured [Speed] is converted to the corresponding encoder
         * preset when [build] is called.
         * Note that you will achieve better quality with a slower preset
         * For more information see: [H.264](https://trac.ffmpeg.org/wiki/Encode/H.264)
         */
        fun speed(speed: Speed) {
            this.speed = speed
        }

        /**

         * Configures video compression quality.
         *
         * [quality] must be between 0 and 100.
         *
         * The quality value is converted to an FFmpeg CRF value:
         *
         * * `0` → CRF 51
         * * `100` → CRF 0
         *
         * Therefore, a higher [quality] produces a lower CRF value and
         * generally results in better output quality.
         */
        fun compress(
            quality: Int = 50,
        ) {
            require(quality in 0..100) {
                "Quality value must be between 0 and 100"
            }

            // Convert the user-friendly 0..100 quality scale
            // into FFmpeg's 0..51 CRF range.
            val fraction = 1F - (quality / 100F)

            this.crf = (51 * fraction).roundToInt()
        }

        /**

         * Sets the coroutine context used during video processing.
         *
         * This can be useful when the caller wants to control the dispatcher
         * or execution context used by [process].
         */
        fun setCoroutineContext(context: CoroutineContext) {
            this.coroutineContext = context
        }

        /**

         * Creates the configured [KEditor] instance.
         *
         * This method is internal because users normally create [KEditor]
         * instances through the DSL-style [KEditor.invoke] function.
         */
        internal fun build(): KEditor =
            KEditor(
                trimConfig = trimConfig,
                filters = filters,
                coroutineContext = coroutineContext,
                removeAudio = removeAudio,
                removeVideo = removeVideo,
                crf = crf,
                preset = speed.preset
            )
    }

    companion object {

        /**
         * Creates a [KEditor] instance using the configuration DSL.
         *
         * Example:
         *
         * ```
         * val editor = KEditor {
         *     trim(10.seconds)
         *     filter(EQFilter())
         *     removeAudio()
         *     compress(quality = 70)
         * }
         * ```
         */
        operator fun invoke(
            block: Config.() -> Unit
        ): KEditor =
            Config()
                .apply(block)
                .build()
    }

    /**

     * Processes the input video and writes the result to [output].
     *
     * The operation is executed inside [coroutineContext] and therefore
     * does not block the caller's coroutine context.
     *
     * @param input Path to the input video.
     * @param output Path where the processed video should be written.
     * @param progress Callback invoked to report processing progress.
     */
    suspend fun process(
        input: String,
        output: String,
        progress: ProgressCallback = {}
    ) = withContext(coroutineContext) {

        PlatformKEditor.applyFilter(
            input = input,
            output = output,
            filters = buildFilters(),
            progress = progress,
            start = trimConfig.start.takeIf {
                it != Duration.ZERO
            },
            end = trimConfig.end.takeIf {
                it != Duration.INFINITE
            },
            removeAudio = removeAudio,
            removeVideo = removeVideo,
            preset = preset,
            crf = crf

        )
    }

    private fun buildFilters() =
        filters.joinToString(
            transform = Filter::build,
            separator = ","
        )
}

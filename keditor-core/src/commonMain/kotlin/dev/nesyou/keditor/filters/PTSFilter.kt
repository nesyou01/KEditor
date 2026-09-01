package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**

 * Changes the presentation timestamp (PTS) of video frames.
 *
 * This filter uses FFmpeg's `setpts` filter to modify the timing
 * of the input video.
 *
 * The [value] is multiplied by the original presentation timestamp:
 *
 * ```
 * setpts=value*PTS
 * ```
 *
 * A value greater than `1.0` slows down the video, while a value
 * between `0.0` and `1.0` speeds it up.
 *
 *
 * @param value Multiplier applied to the original presentation timestamp.
 * `1.0` keeps the original playback speed.
 */
class PTSFilter(
    private val value: Float
) : Filter {

    /**

     * Builds the FFmpeg `setpts` filter expression.
     */
    override fun build(): String =
        "setpts=${value}*PTS"
}

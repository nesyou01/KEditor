package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**
 * Applies a fade-in or fade-out effect to a video.
 *
 * This filter uses FFmpeg's `fade` video filter and supports configuring
 * the fade type, start time, duration, and color.
 *
 * @param type Type of fade effect to apply.
 * @param startTime Time in seconds at which the fade starts.
 * @param duration Duration of the fade effect in seconds.
 * @param colorHex Color of the fade in hexadecimal format.
 * The leading `#` is optional. Defaults to `FF0000` (red).
 */
class FadeFilter(
    private val type: Type,
    private val startTime: Double,
    private val duration: Double,
    private val colorHex: String = "FF0000"
) : Filter {

    /**
     * Specifies whether the filter should fade the video in or out.
     */
    enum class Type {
        /** Gradually fades from the specified color to the video. */
        IN,

        /** Gradually fades from the video to the specified color. */
        OUT
    }

    /**
     * Builds the FFmpeg `fade` filter expression.
     */
    override fun build(): String =
        "fade=t=${type.name.lowercase()}:st=$startTime:d=$duration:color=${colorHex.removePrefix("#")}"
}

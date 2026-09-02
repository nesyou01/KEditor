package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**
 * Applies a box blur effect to the video.
 *
 * The filter allows independent configuration of the luma and chroma
 * blur radius and power.
 *
 * The generated FFmpeg expression uses the `boxblur` filter.
 *
 * Example:
 *
 * ```
 * BoxBlurFilter(
lumaRadius = 5.0,
lumaPower = 2
 * )
 * ```
 *
 * Produces:
 *
 * ```
 * boxblur=luma_radius=5.0:luma_power=2
 * ```
 *
 * @param lumaRadius Radius of the blur applied to the luma component.
 * Defaults to `2.0`.
 * @param lumaPower Number of times the luma blur is applied.
 * Defaults to `2`.
 * @param chromaRadius Optional radius of the blur applied to the chroma
 * components. If `null`, the FFmpeg default is used.
 * @param chromaPower Optional number of times the chroma blur is applied.
 * If `null`, the FFmpeg default is used.
 */
class BoxBlurFilter(
    private val lumaRadius: Double = 2.0,
    private val lumaPower: Int = 2,
    private val chromaRadius: Double? = null,
    private val chromaPower: Int? = null
) : Filter {

    /**
     * Builds the FFmpeg `boxblur` filter expression.
     */
    override fun build(): String = buildString {
        append("boxblur=")
        append("luma_radius=$lumaRadius")
        append(":luma_power=$lumaPower")

        chromaRadius?.let {
            append(":chroma_radius=$it")
        }

        chromaPower?.let {
            append(":chroma_power=$it")
        }
    }
}

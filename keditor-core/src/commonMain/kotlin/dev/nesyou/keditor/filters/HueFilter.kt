package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**

 * Adjusts the hue and saturation of a video.
 *
 * This filter uses FFmpeg's `hue` video filter to modify the color
 * properties of the input video.
 *
 *
 * @param hue Hue rotation in degrees. `0.0` leaves the hue unchanged.
 * @param saturation Saturation multiplier. `1.0` leaves the saturation
 * unchanged, while values below `1.0` reduce saturation and values above
 * `1.0` increase it.
 */
class HueFilter(
    private val hue: Double = 0.0,
    private val saturation: Double = 1.0
) : Filter {

    /**
     * Builds the FFmpeg `hue` filter expression.
     */
    override fun build(): String =
        "hue=h=$hue:s=$saturation"
}

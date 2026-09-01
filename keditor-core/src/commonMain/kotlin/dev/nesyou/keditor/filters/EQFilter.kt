package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**
 * Adjusts the brightness, contrast, saturation, and gamma of a video.
 *
 * This filter uses FFmpeg's `eq` (equalizer) video filter.
 *
 *
 * @param brightness Brightness adjustment. `0.0` leaves brightness unchanged.
 * @param contrast Contrast multiplier. `1.0` leaves contrast unchanged.
 * @param saturation Saturation multiplier. `1.0` leaves saturation unchanged.
 * @param gamma Gamma correction value. `1.0` leaves gamma unchanged.
 */
class EQFilter(
    private val brightness: Double = 0.0,
    private val contrast: Double = 1.0,
    private val saturation: Double = 1.0,
    private val gamma: Double = 1.0
) : Filter {

    /**
     * Builds the FFmpeg `eq` filter expression.
     */
    override fun build(): String =
        "eq=brightness=$brightness:contrast=$contrast:saturation=$saturation:gamma=$gamma"
}

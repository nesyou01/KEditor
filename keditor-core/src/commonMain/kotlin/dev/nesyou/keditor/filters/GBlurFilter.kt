package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**
 * Applies a Gaussian blur effect to a video.
 *
 * This filter uses FFmpeg's `gblur` video filter to blur the input
 * using a Gaussian distribution.
 *
 * A higher [sigma] value produces a stronger blur, while [steps]
 * controls the number of times the blur operation is applied.
 *
 * @param sigma Standard deviation of the Gaussian blur.
 * Defaults to `1.0`.
 * @param steps Number of times the Gaussian blur is applied.
 * Defaults to `1`.
 */
class GBlurFilter(
    private val sigma: Double = 1.0,
    private val steps: Int = 1
) : Filter {

    /**
     * Builds the FFmpeg `gblur` filter expression.
     */
    override fun build(): String =
        "gblur=sigma=$sigma:steps=$steps"

}

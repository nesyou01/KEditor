package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**
 * Resizes a video to the specified dimensions.
 *
 * This filter uses FFmpeg's `scale` video filter to change the width
 * and height of the input video.
 *
 *
 * @param width Width of the resulting video in pixels.
 * @param height Height of the resulting video in pixels.
 */
class ScaleFilter(
    private val width: Int,
    private val height: Int
) : Filter {

    /**
     * Builds the FFmpeg `scale` filter expression.
     */
    override fun build(): String =
        "scale=$width:$height"
}

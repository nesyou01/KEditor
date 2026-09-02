package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**
 * Changes the frame rate of a video.
 *
 * This filter uses FFmpeg's `fps` video filter to convert the input
 * video to the specified number of frames per second.
 *
 *
 * @param fps Target frame rate in frames per second.
 */
class FPSFilter(
    private val fps: Int
) : Filter {

    /**
     * Builds the FFmpeg `fps` filter expression.
     */
    override fun build(): String =
        "fps=$fps"
}

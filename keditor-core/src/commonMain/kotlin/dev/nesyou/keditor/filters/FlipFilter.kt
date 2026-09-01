package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**
 * Flips a video horizontally or vertically.
 *
 * This filter uses FFmpeg's `hflip` or `vflip` video filter depending
 * on the selected [FlipDirection].
 *
 * @param direction Direction in which the video should be flipped.
 */
class FlipFilter(
    private val direction: FlipDirection
) : Filter {

    /**
     * Builds the corresponding FFmpeg flip filter expression.
     */
    override fun build(): String =
        when (direction) {
            FlipDirection.HORIZONTAL -> "hflip"
            FlipDirection.VERTICAL -> "vflip"
        }

    /**
     * Specifies the direction in which the video should be flipped.
     */
    enum class FlipDirection {

        /** Flips the video from left to right. */
        HORIZONTAL,

        /** Flips the video from top to bottom. */
        VERTICAL
    }
}


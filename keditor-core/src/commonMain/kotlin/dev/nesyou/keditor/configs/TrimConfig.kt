package dev.nesyou.keditor.configs

import kotlin.time.Duration


//
// Created by Youness Lagmah on 8/24/26.
//

/**
 * Defines the portion of a video that should be processed.
 *
 * @param start Start position of the trim. Defaults to [Duration.ZERO].
 * @param end End position of the trim. Defaults to [Duration.INFINITE],
 * meaning that the video is processed until the end.
 */
class TrimConfig(
    val start: Duration = Duration.ZERO,
    val end: Duration = Duration.INFINITE
) {

    companion object {

        /**
         * Represents an unset trim configuration.
         *
         * When no trimming is required, [KEditor] uses this value to
         * process the video from the beginning to the end.
         */
        val UNSET = TrimConfig()

    }
}


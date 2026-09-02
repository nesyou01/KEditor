package dev.nesyou.keditor.configs

import kotlin.jvm.JvmInline

//
// Created by Youness Lagmah on 8/31/26.
//

/**
 * Represents the encoding speed/preset used when processing a video.
 *
 * The available presets provide a trade-off between encoding speed
 * and compression efficiency. Faster presets generally require more
 * bitrate/file size for the same quality, while slower presets can
 * achieve better compression at the cost of longer processing time.
 *
 * The value is backed by the corresponding FFmpeg encoder preset name.
 *
 * Example:
 *
 * val editor = KEditor {
speed(Speed.Fast)
 * }
 */
@JvmInline
value class Speed(
    /**
     * FFmpeg encoder preset name.
     */
    val preset: String
) {

    companion object {

        /** Fastest encoding preset. */
        val UltraFast = Speed("ultrafast")

        /** Very fast encoding preset. */
        val SuperFast = Speed("superfast")

        /** Fast encoding preset. */
        val VeryFast = Speed("veryfast")

        /** Faster encoding preset. */
        val Faster = Speed("faster")

        /** Fast encoding preset. */
        val Fast = Speed("fast")

        /** Default balanced encoding preset. */
        val Medium = Speed("medium")

        /** Slow encoding preset with better compression efficiency. */
        val Slow = Speed("slow")

        /** Slower encoding preset with better compression efficiency. */
        val Slower = Speed("slower")

        /** Very slow encoding preset with better compression efficiency. */
        val VerySlow = Speed("veryslow")
    }

}

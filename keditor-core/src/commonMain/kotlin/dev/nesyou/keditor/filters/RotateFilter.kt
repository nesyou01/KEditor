package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

/**

 * Rotates a video by the specified angle.
 *
 * This filter uses FFmpeg's `rotate` video filter. Areas exposed by
 * the rotation are filled with the specified color.
 *
 * @param angle Rotation angle in radians. A positive value rotates
 * the video clockwise.
 * @param fillColorHex Color used to fill the empty areas created by
 * the rotation. The leading `#` is optional. Defaults to `FF0000` (red).
 */
class RotateFilter(
    private val angle: Double,
    private val fillColorHex: String = "FF0000"
) : Filter {

    /**
     * Builds the FFmpeg `rotate` filter expression.
     */
    override fun build(): String =
        "rotate=$angle:fillcolor=${fillColorHex.removePrefix("#")}"
}

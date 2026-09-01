package dev.nesyou.keditor.filters

import kotlin.math.roundToInt

//
// Created by Youness Lagmah on 8/24/26.
//

/**

 * Crops a video to a specified rectangular region.
 *
 * The crop area can be defined either by its position and dimensions
 * or by specifying its left, right, top, and bottom boundaries.
 *
 * Example:
 *
 * ```
 * CropFilter(
startX = 100.0,
startY = 50.0,
width = 500,
height = 500
 * )
 * ```
 *
 * Produces:
 *
 * ```
 * crop=500:500:100.0:50.0
 * ```
 *
 * @param startX Horizontal position of the top-left corner of the crop area.
 * @param startY Vertical position of the top-left corner of the crop area.
 * @param width Width of the resulting cropped video.
 * @param height Height of the resulting cropped video.
 */
class CropFilter(
    private val startX: Double = 0.0,
    private val startY: Double = 0.0,
    private val width: Int,
    private val height: Int
) : Filter {

    /**

     * Creates a crop filter using the boundaries of the crop area.
     *
     * The resulting width is calculated as `end - start`, while the
     * resulting height is calculated as `bottom - top`.
     *
     * @param start Left boundary of the crop area.
     * @param end Right boundary of the crop area.
     * @param top Top boundary of the crop area.
     * @param bottom Bottom boundary of the crop area.
     */
    constructor(
        start: Double,
        end: Double,
        top: Double,
        bottom: Double
    ) : this(
        startX = start,
        startY = top,
        width = (end - start).roundToInt(),
        height = (bottom - top).roundToInt()
    )

    /**
     * Builds the FFmpeg `crop` filter expression.
     */
    override fun build(): String =
        "crop=$width:$height:${startX}:${startY}"
}

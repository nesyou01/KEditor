package dev.nesyou.keditor.filters

import kotlin.math.roundToInt

//
// Created by Youness Lagmah on 8/24/26.
//

class CropFilter(
    private val startX: Double,
    private val startY: Double,
    private val width: Int,
    private val height: Int
) : Filter {

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

    override fun build(): String =
        "crop=$width:$height:$startX:$startY"

}
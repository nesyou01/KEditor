package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class ScaleFilter(
    private val width: Int,
    private val height: Int
) : Filter {

    override fun build(): String =
        "scale=$width:$height"

}
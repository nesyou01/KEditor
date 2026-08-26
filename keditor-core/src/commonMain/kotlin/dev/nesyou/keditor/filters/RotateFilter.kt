package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class RotateFilter (
    private val angle: Double,
    private val fillColorHex: String = "FF0000"
) : Filter {

    override fun build(): String =
        "rotate=$angle:fillcolor=${fillColorHex.removePrefix("#")}"

}
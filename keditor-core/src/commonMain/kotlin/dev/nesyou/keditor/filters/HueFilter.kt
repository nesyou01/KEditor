package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class HueFilter(
    private val hue: Double = 0.0,
    private val saturation: Double = 1.0
) : Filter {

    override fun build(): String =
        "hue=h=$hue:s=$saturation"
}

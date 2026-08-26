package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class EQFilter(
    private val brightness: Double = 0.0,
    private val contrast: Double = 1.0,
    private val saturation: Double = 1.0,
    private val gamma: Double = 1.0
) : Filter {

    override fun build(): String =
        "eq=brightness=$brightness:contrast=$contrast:saturation=$saturation:gamma=$gamma"
}
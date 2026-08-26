package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class GBlurFilter(
    private val sigma: Double = 1.0,
    private val steps: Int = 1
) : Filter {

    override fun build(): String =
        "gblur=sigma=$sigma:steps=$steps"
}

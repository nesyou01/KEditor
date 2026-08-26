package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class FPSFilter(
    private val fps: Int
) : Filter {

    override fun build(): String =
        "fps=$fps"

}
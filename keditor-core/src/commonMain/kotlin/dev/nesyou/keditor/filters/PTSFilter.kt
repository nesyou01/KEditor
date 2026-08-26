package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class PTSFilter(
    private val value: Float
) : Filter {


    override fun build(): String =
        "setpts=${value}*PTS"

}

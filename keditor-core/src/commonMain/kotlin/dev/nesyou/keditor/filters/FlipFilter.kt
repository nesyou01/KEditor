package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class FlipFilter(
    private val direction: FlipDirection
) : Filter {

    override fun build(): String =
        when (direction) {
            FlipDirection.HORIZONTAL -> "hflip"
            FlipDirection.VERTICAL -> "vflip"
        }

    enum class FlipDirection {
        HORIZONTAL,
        VERTICAL
    }
}

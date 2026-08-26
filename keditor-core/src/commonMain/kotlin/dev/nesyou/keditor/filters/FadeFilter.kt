package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class FadeFilter(
    private val type: Type,
    private val startTime: Double,
    private val duration: Double,
    private val colorHex: String = "FF0000"
) : Filter {

    enum class Type {
        IN,
        OUT
    }

    override fun build(): String =
        "fade=t=${type.name.lowercase()}:st=$startTime:d=$duration:color=${colorHex.removePrefix("#")}"
}

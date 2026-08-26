package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/26/26.
//

class BoxBlurFilter(
    private val lumaRadius: Double = 2.0,
    private val lumaPower: Int = 2,
    private val chromaRadius: Double? = null,
    private val chromaPower: Int? = null
) : Filter {

    override fun build(): String = buildString {
        append("boxblur=")
        append("luma_radius=$lumaRadius")
        append(":luma_power=$lumaPower")

        chromaRadius?.let {
            append(":chroma_radius=$it")
        }

        chromaPower?.let {
            append(":chroma_power=$it")
        }
    }
}

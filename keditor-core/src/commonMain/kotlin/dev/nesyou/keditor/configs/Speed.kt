package dev.nesyou.keditor.configs

import kotlin.jvm.JvmInline

//
// Created by Youness Lagmah on 8/31/26.
//

@JvmInline
value class Speed(val preset: String) {

    companion object {

        val UltraFast = Speed("ultrafast")
        val SuperFast = Speed("superfast")
        val VeryFast = Speed("veryfast")
        val Faster = Speed("faster")
        val Fast = Speed("fast")
        val Medium = Speed("medium")
        val Slow = Speed("slow")
        val Slower = Speed("Slower")
        val VerySlow = Speed("veryslow")

    }

}
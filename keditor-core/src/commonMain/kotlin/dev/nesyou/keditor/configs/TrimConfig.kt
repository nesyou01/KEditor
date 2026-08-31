package dev.nesyou.keditor.configs

import kotlin.time.Duration


//
// Created by Youness Lagmah on 8/24/26.
//

class TrimConfig(
    val start: Duration = Duration.ZERO,
    val end: Duration = Duration.INFINITE
) {

    companion object {
        val UNSET = TrimConfig()
    }

}
package dev.nesyou.keditor

//
// Created by Youness Lagmah on 8/25/26.
//

internal expect object PlatformKEditor {

    fun applyFilter(input: String, output: String, filters: String)

}

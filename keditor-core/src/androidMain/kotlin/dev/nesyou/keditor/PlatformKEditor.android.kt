package dev.nesyou.keditor

import dev.nesyou.keditor.ffmpeg.AndroidMediaEditor

internal actual object PlatformKEditor {

    actual fun applyFilter(input: String, output: String, filters: String) {
        AndroidMediaEditor.applyFilter(input, output, filters)
    }

}
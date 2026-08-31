package dev.nesyou.keditor

import dev.nesyou.keditor.callbacks.ProgressCallback
import kotlin.time.Duration

//
// Created by Youness Lagmah on 8/25/26.
//

internal expect object PlatformKEditor {

    fun applyFilter(
        input: String,
        output: String,
        filters: String,
        start: Duration?,
        end: Duration?,
        progress: ProgressCallback
    )

}

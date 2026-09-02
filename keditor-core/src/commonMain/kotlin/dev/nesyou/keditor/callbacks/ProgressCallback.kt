package dev.nesyou.keditor.callbacks

//
// Created by Youness Lagmah on 8/26/26.
//

/**
 * Callback used to receive video processing progress updates.
 *
 * The callback is invoked during video processing with the current
 * progress represented as a value between `0.0` and `1.0`.
 *
 * For example:
 *```
 * val editor = KEditor {
// configuration
 * }
 *
 * editor.process(
input = "input.mp4",
output = "output.mp4"
 * ) { progress ->
println("Progress: ${progress * 100}%")
 * }
 *```
 * Because this is a [fun interface], it can be used directly with a
 * lambda expression.
 */
fun interface ProgressCallback {

    /**
     * Called when the processing progress changes.
     *
     * @param progress Current processing progress, where `0.0` represents
     * no progress and `1.0` represents 100% completion.
     */
    fun onProgress(progress: Float)
}

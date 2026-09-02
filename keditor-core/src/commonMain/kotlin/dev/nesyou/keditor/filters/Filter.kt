package dev.nesyou.keditor.filters

//
// Created by Youness Lagmah on 8/24/26.
//

/**
 * Represents a video filter that can be applied by [KEditor].
 *
 * Implementations are responsible for converting their configuration
 * into an FFmpeg filter expression.
 *
 * For example, a filter implementation might produce:
 *
 * scale=1280:720
 *
 * or:
 *
 * crop=500:500:0:0
 *
 * Multiple filters are combined by [KEditor] into a comma-separated
 * FFmpeg filter chain.
 */
interface Filter {

    /**
     * Builds the FFmpeg filter expression for this filter.
     *
     * @return An FFmpeg-compatible filter expression.
     */
    fun build(): String
}

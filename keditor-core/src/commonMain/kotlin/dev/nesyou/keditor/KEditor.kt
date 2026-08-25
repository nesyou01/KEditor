package dev.nesyou.keditor

import dev.nesyou.keditor.configs.TrimConfig
import dev.nesyou.keditor.filters.Filter

//
// Created by Youness Lagmah on 8/24/26.
//

class KEditor private constructor(
    private val trimConfig: TrimConfig,
    private val filters: Collection<Filter>,
) {

    class Builder {
        private var trimConfig: TrimConfig = TrimConfig.UNSET
        private var filters: Collection<Filter> = emptyList()

        fun setTrimConfig(trimConfig: TrimConfig): Builder = apply {
            this.trimConfig = trimConfig
        }

        fun setFilters(vararg filters: Filter): Builder = apply {
            this.filters = filters.asList()
        }

        fun build(): KEditor =
            KEditor(
                trimConfig = trimConfig,
                filters = filters
            )

    }


    fun start(input: String, output: String) {
        PlatformKEditor.applyFilter(
            input = input,
            output = output,
            filters = buildFilters()
        )
    }


    private fun buildFilters() =
        this.filters.joinToString(transform = Filter::build, separator = ",")
}
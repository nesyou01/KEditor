package dev.nesyou.keditor

import dev.nesyou.keditor.callbacks.ProgressCallback
import dev.nesyou.keditor.configs.TrimConfig
import dev.nesyou.keditor.filters.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

//
// Created by Youness Lagmah on 8/24/26.
//

class KEditor private constructor(
    private val trimConfig: TrimConfig,
    private val filters: Collection<Filter>,
    private val coroutineContext: CoroutineContext
) {

    class Builder {
        private var trimConfig: TrimConfig = TrimConfig.UNSET

        private var filters: Collection<Filter> = emptyList()

        private var coroutineContext: CoroutineContext = Dispatchers.IO


        fun setTrimConfig(trimConfig: TrimConfig): Builder = apply {
            this.trimConfig = trimConfig
        }

        fun setFilters(vararg filters: Filter): Builder = apply {
            this.filters = filters.asList()
        }

        fun setCoroutineContext(context: CoroutineContext) = apply {
            this.coroutineContext = context
        }

        fun build(): KEditor =
            KEditor(
                trimConfig = trimConfig,
                filters = filters,
                coroutineContext = coroutineContext
            )
    }


    suspend fun start(
        input: String,
        output: String,
        progress: ProgressCallback = {}
    ) =
        withContext(coroutineContext) {
            PlatformKEditor.applyFilter(
                input = input,
                output = output,
                filters = buildFilters(),
                progress = progress
            )
        }


    private fun buildFilters() =
        this.filters.joinToString(transform = Filter::build, separator = ",")
}
package dev.nesyou.keditor

import dev.nesyou.keditor.callbacks.ProgressCallback
import dev.nesyou.keditor.configs.TrimConfig
import dev.nesyou.keditor.filters.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

//
// Created by Youness Lagmah on 8/24/26.
//

class KEditor private constructor(
    private val trimConfig: TrimConfig,
    private val filters: Collection<Filter>,
    private val coroutineContext: CoroutineContext
) {

    class Config {
        private var trimConfig: TrimConfig = TrimConfig.UNSET
        var coroutineContext: CoroutineContext = Dispatchers.IO

        private val filters = mutableListOf<Filter>()


        fun trim(config: TrimConfig) {
            this.trimConfig = config
        }

        fun trim(start: Duration, end: Duration) {
            trim(
                TrimConfig(
                    start = start,
                    end = end
                )
            )
        }

        fun trim(end: Duration) {
            trim(start = Duration.ZERO, end = end)
        }

        fun filters(vararg filters: Filter) {
            this.filters += filters
        }

        fun filter(filter: Filter) {
            this.filters += filter
        }

        internal fun build(): KEditor =
            KEditor(
                trimConfig = trimConfig,
                filters = filters,
                coroutineContext = coroutineContext
            )
    }

    companion object {

        operator fun invoke(
            block: Config.() -> Unit
        ): KEditor =
            Config().apply(block).build()
    }

    suspend fun process(
        input: String,
        output: String,
        progress: ProgressCallback = {}
    ) = withContext(coroutineContext) {
        PlatformKEditor.applyFilter(
            input = input,
            output = output,
            filters = buildFilters(),
            progress = progress,
            start = trimConfig.start.takeIf { it != Duration.ZERO },
            end = trimConfig.end.takeIf { it != Duration.INFINITE }
        )
    }


    private fun buildFilters() =
        filters.joinToString(transform = Filter::build, separator = ",")
}
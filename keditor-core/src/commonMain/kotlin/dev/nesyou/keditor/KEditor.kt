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
    private val coroutineContext: CoroutineContext,
    private val removeAudio: Boolean,
    private val removeVideo: Boolean
) {

    class Config {
        private var trimConfig: TrimConfig = TrimConfig.UNSET

        private var removeAudio: Boolean = false

        private var removeVideo: Boolean = false

        private var coroutineContext: CoroutineContext = Dispatchers.IO

        private val filters = mutableListOf<Filter>()


        fun trim(config: TrimConfig) {
            this.trimConfig = config
        }


        fun removeAudio() {
            this.removeAudio = true
        }

        fun enableAudio() {
            this.removeAudio = false
        }

        fun removeVideo() {
            this.removeVideo = true
        }

        fun enableVideo() {
            this.removeVideo = false
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
            trim(
                start = Duration.ZERO,
                end = end
            )
        }

        fun filters(vararg filters: Filter) {
            this.filters += filters
        }

        fun filter(filter: Filter) {
            this.filters += filter
        }

        fun setCoroutineContext(context: CoroutineContext) {
            this.coroutineContext = context
        }

        internal fun build(): KEditor =
            KEditor(
                trimConfig = trimConfig,
                filters = filters,
                coroutineContext = coroutineContext,
                removeAudio = removeAudio,
                removeVideo = removeVideo
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
            end = trimConfig.end.takeIf { it != Duration.INFINITE },
            removeAudio = removeAudio,
            removeVideo = removeVideo
        )
    }


    private fun buildFilters() =
        filters.joinToString(transform = Filter::build, separator = ",")
}
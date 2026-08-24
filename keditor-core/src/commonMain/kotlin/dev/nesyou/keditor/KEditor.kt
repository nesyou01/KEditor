package dev.nesyou.keditor

import dev.nesyou.keditor.configs.TrimConfig
import dev.nesyou.keditor.filters.Filter

//
// Created by Youness Lagmah on 8/24/26.
//

class KEditor private constructor(
) {

    class Builder {
        private var trimConfig: TrimConfig = TrimConfig.UNSET
        private var filters: Collection<Filter> = emptyList()

        fun setTrimConfig(trimConfig: TrimConfig) {
            this.trimConfig = trimConfig
        }

        fun setFilters(vararg filters: Filter) {
            this.filters = filters.asList()
        }

        fun build(): KEditor =
            KEditor()

    }


    fun start(input: String, output: String) {

    }

}
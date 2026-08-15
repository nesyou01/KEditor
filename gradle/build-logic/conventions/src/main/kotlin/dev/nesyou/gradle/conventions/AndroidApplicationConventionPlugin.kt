package dev.nesyou.gradle.conventions

import dev.nesyou.gradle.conventions.utils.configureApp
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }

            configureApp()
        }
    }
}
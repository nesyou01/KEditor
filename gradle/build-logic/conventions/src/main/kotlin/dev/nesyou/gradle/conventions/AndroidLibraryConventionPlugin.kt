package dev.nesyou.gradle.conventions

import dev.nesyou.gradle.conventions.utils.configureAndroidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

            configureAndroidLibrary()
        }
    }
}
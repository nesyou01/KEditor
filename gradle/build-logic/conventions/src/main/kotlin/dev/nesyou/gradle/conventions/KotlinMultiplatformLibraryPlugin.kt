package dev.nesyou.gradle.conventions

import dev.nesyou.gradle.conventions.utils.configureAndroidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.kotlin.multiplatform.library")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            configureAndroidLibrary()
        }
    }

}

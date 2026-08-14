package dev.nesyou.gradle.conventions.utils

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.configureAndroidLibrary() {
    library {
        compileSdk {
            version = release(libs.getVersionAsInt("android-compileSdk"))
        }

        configureJava()

        defaultConfig {
            minSdk = libs.getVersionAsInt("android-minSdk")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}


fun Project.library(action: LibraryExtension.() -> Unit) =
    extensions.configure<LibraryExtension>(action)




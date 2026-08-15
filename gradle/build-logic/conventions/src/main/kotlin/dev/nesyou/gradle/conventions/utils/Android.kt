package dev.nesyou.gradle.conventions.utils

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.configureApp() {
    application {
        compileSdk {
            version = release(libs.getVersionAsInt("android-compileSdk"))
        }

        configureJava()

        defaultConfig {
            minSdk = libs.getVersionAsInt("android-minSdk")
            targetSdk = libs.getVersionAsInt("android-targetSdk")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

fun KotlinMultiplatformExtension.configureAndroidLibrary() {
    androidLibrary {
        compileSdk {
            version = release(project.libs.getVersionAsInt("android-compileSdk"))
        }

        minSdk = project.libs.getVersionAsInt("android-minSdk")
    }
}


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

fun Project.application(action: ApplicationExtension.() -> Unit) =
    extensions.configure<ApplicationExtension>(action)

fun Project.library(action: LibraryExtension.() -> Unit) =
    extensions.configure<LibraryExtension>(action)


private fun KotlinMultiplatformExtension.androidLibrary(action: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) =
    extensions.configure<KotlinMultiplatformAndroidLibraryTarget>(action)

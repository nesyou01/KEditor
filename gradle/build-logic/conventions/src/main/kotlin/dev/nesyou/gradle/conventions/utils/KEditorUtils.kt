package dev.nesyou.gradle.conventions.utils

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

//
// Created by Youness Lagmah on 8/19/26.
//


private fun KotlinNativeTarget.nativeDir(dir: String) =
    when (konanTarget) {
        KonanTarget.IOS_ARM64 ->
            project.file("../native/${dir}/darwin/iphoneos/arm64")

        KonanTarget.IOS_SIMULATOR_ARM64 ->
            project.file("../native/${dir}/darwin/iphonesimulator/arm64")

        else -> error("Unsupported target")
    }

fun createKEditor(taget: KotlinNativeTarget) {
    val ffmpegDir = taget.nativeDir("ffmpeg")
    val x264Dir = taget.nativeDir("x264")

    taget.compilations.getByName("main") {
        cinterops {
            create("keditor") {
                extraOpts("-libraryPath", x264Dir.resolve("lib").absolutePath)
                extraOpts("-libraryPath", ffmpegDir.resolve("lib").absolutePath)

                definitionFile.set(
                    project.file("src/iosMain/cinterop/keditor.def")
                )

                includeDirs(
                    ffmpegDir.resolve("include"),
                    x264Dir.resolve("include")
                )
            }
        }
    }
}
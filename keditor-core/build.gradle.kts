import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.kotlin"
version = "1.0.0"

kotlin {
    android {
        namespace = "dev.nesyou.keditor"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    iosTargets.forEach { target ->
        val ffmpegDir = when (target.konanTarget) {
            KonanTarget.IOS_ARM64 ->
                project.file("../native/ffmpeg/darwin/iphoneos/arm64")

            KonanTarget.IOS_SIMULATOR_ARM64 ->
                project.file("../native/ffmpeg/darwin/iphonesimulator/arm64")

            else -> error("Unsupported target")
        }
        val x264Dir = when (target.konanTarget) {
            KonanTarget.IOS_ARM64 ->
                project.file("../native/x264/darwin/iphoneos/arm64")

            KonanTarget.IOS_SIMULATOR_ARM64 ->
                project.file("../native/x264/darwin/iphonesimulator/arm64")

            else -> error("Unsupported target")
        }

        target.compilations.getByName("main") {
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

    sourceSets {
        androidMain {
            dependencies {
                api(projects.keditorFfmpeg)
            }
        }

        commonMain {
            dependencies {
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "keditor", version.toString())

    pom {
        name = "KEditor"
        description = "A library."
        inceptionYear = "2024"
        url = "https://github.com/XXX"
        licenses {
            license {
                name = "XXX"
                url = "YYY"
                distribution = "ZZZ"
            }
        }
        developers {
            developer {
                id = "XXX"
                name = "YYY"
                url = "ZZZ"
            }
        }
        scm {
            url = "XXX"
            connection = "YYY"
            developerConnection = "ZZZ"
        }
    }
}

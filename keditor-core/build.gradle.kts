import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
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

    iosArm64()

    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter {
            it.konanTarget == KonanTarget.IOS_ARM64 ||
                    it.konanTarget == KonanTarget.IOS_SIMULATOR_ARM64
        }
        .forEach { target ->
            target.compilations.getByName("main") {
                cinterops {
                    create("keditor") {
                        definitionFile.set(
                            project.file(
                                "src/iosMain/cinterop/ffmpeg.def"
                            )
                        )

                        compilerOpts(
                            "-I${project.file("src/iosMain/cpp").absolutePath}"
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

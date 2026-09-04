import dev.nesyou.gradle.conventions.utils.createKEditor

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("dev.nesyou.kmp.library")

    alias(libs.plugins.vanniktech.mavenPublish)
}

tasks.register<Exec>("buildNativeLibs") {
    description = "Build native libraries"

    commandLine("bash", "../scripts/setup.sh")
}


tasks.matching {
    it.name == "commonizeNativeDistribution"
}.configureEach {
    dependsOn("buildNativeLibs")
}


kotlin {
    android {
        namespace = "dev.nesyou.keditor"
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach(::createKEditor)

    sourceSets {
        androidMain {
            dependencies {
                api(projects.keditorFfmpeg)
            }
        }

        commonMain {
            dependencies {
                api(libs.coroutines.core)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()
}

import dev.nesyou.gradle.conventions.utils.createKEditor

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("dev.nesyou.kmp.library")

    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.kotlin"
version = "1.0.0"

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

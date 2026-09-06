plugins {
    id("dev.nesyou.android.library")

    alias(libs.plugins.vanniktech.mavenPublish)
}

android {
    namespace = "dev.nesyou.editor.ffmpeg"

    defaultConfig {
        ndk {
            abiFilters += listOf(
                "arm64-v8a",
                "x86_64"
            )
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(null, "keditor-ffmpeg", null)

    pom {
        name = "KEditorFFmpeg"
    }
}


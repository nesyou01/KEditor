plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    compileOnly(libs.multiplatform.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("AndroiLibrary") {
            id = "dev.nesyou.android.library"
            implementationClass = "dev.nesyou.gradle.conventions.AndroidLibraryConventionPlugin"
        }

        register("AndroidApp") {
            id = "dev.nesyou.android.app"
            implementationClass = "dev.nesyou.gradle.conventions.AndroidApplicationConventionPlugin"
        }

        register("Compose") {
            id = "dev.nesyou.compose"
            implementationClass = "dev.nesyou.gradle.conventions.ComposeConventionPlugin"
        }

        register("kotlinMultiplatform") {
            id = "dev.nesyou.kmp"
            implementationClass = "dev.nesyou.gradle.conventions.KotlinMultiplatformConventionPlugin"
        }

        register("kotlinMultiplatformLibrary") {
            id = "dev.nesyou.kmp.library"
            implementationClass = "dev.nesyou.gradle.conventions.KotlinMultiplatformLibraryPlugin"
        }
    }
}
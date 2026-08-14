plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
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
    }
}
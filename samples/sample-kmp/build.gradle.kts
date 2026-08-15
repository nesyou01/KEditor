plugins {
    id("dev.nesyou.kmp")
    id("dev.nesyou.kmp.library")
    id("dev.nesyou.compose")
}

kotlin {
    android {
        namespace = "dev.nesyou.keditor.sample.kmp"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.ui)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)

            }
        }
    }
}
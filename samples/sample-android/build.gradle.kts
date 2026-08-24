plugins {
    id("dev.nesyou.android.app")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.nesyou.keditor.sample"

    defaultConfig {
        applicationId = "dev.nesyou.keditor.sample"

        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    implementation(projects.samples.sampleKmp)
}

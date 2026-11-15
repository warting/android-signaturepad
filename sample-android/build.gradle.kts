plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "se.warting.signaturesample"
    compileSdk = 37
    defaultConfig {
        applicationId = "se.warting.signaturesample"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":sample-multiplatform"))
    implementation(libs.androidx.activity.activity.compose)
}

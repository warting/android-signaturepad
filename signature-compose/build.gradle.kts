plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose") version "1.12.2"
    alias(libs.plugins.compose.compiler)
    id("com.android.kotlin.multiplatform.library")
    id("maven-publish")
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(21)
    android {
        namespace = "se.warting.signaturecompose"
        compileSdk = 37
        minSdk = 23
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies {
            api(project(":signature-engine"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.compose.ui:ui-test:1.12.2")
        }
        val desktopTest by getting {
            dependencies { implementation(compose.desktop.currentOs) }
        }
    }
}

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(21)
    jvm()
    iosArm64()
    iosSimulatorArm64()
    wasmJs {
        browser()
        nodejs()
    }
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

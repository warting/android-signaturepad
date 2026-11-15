plugins {
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(21)
    android {
        namespace = "se.warting.signaturesample.shared"
        compileSdk = 37
        minSdk = 23
    }
    jvm("desktop")
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "SignatureSample"
            isStatic = true
        }
    }
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":signature-compose"))
            implementation(compose.material3)
        }
        val desktopMain by getting {
            dependencies { implementation(compose.desktop.currentOs) }
        }
    }
}

compose.desktop {
    application { mainClass = "se.warting.signaturesample.MainKt" }
}

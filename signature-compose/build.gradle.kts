plugins {
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("com.android.kotlin.multiplatform.library")
    id("maven-publish")
    alias(libs.plugins.com.vanniktech.maven.publish)
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
    wasmJs {
        browser { testTask { useKarma { useChromeHeadless() } } }
        // Compose's browser test harness needs webpack to load the Skiko runtime.
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":signature-engine"))
            api(compose.runtime)
            implementation(compose.foundation)
            api(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.multiplatform.ui.test)
        }
        val desktopTest by getting {
            dependencies { implementation(compose.desktop.currentOs) }
        }
    }
}

mavenPublishing {

    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("signature-compose")
        description.set("Native Compose signature canvas for Android, iOS, desktop and WebAssembly")
        inceptionYear.set("2021")
        url.set("https://github.com/warting/android-signaturepad/")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("warting")
                name.set("Stefan Wärting")
                url.set("https://github.com/warting/")
            }
        }
        scm {
            url.set("https://github.com/warting/android-signaturepad/")
            connection.set("scm:git:git://github.com/warting/android-signaturepad.git")
            developerConnection.set("scm:git:ssh://git@github.com/warting/android-signaturepad.git")
        }
    }
}

tasks.named<Test>("desktopTest") {
    inputs.files(fileTree("src/desktopTest/reference"))
    inputs.property("updateScreenshots", providers.environmentVariable("UPDATE_SIGNATURE_SCREENSHOTS").orElse("false"))
    outputs.dir(layout.buildDirectory.dir("screenshots"))
}

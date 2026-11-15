plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.com.vanniktech.maven.publish)
}

group = rootProject.group
version = rootProject.version

android {
    namespace = "se.warting.signatureink"
    compileSdk = 37
    defaultConfig { minSdk = 23 }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    api(libs.androidx.ink.brush)
    api(libs.androidx.ink.strokes)
    implementation(libs.androidx.ink.authoring)
    implementation(libs.androidx.ink.rendering)
    implementation(libs.androidx.input.motionprediction)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.runtime)
}

mavenPublishing {

    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("Signature - Ink")
        description.set("Optional Android signature renderer using Jetpack Ink with stylus pressure and low-latency input")
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

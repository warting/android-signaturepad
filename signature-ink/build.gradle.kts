plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.com.vanniktech.maven.publish)
}

detekt {
    autoCorrect = true
    buildUponDefaultConfig = true
}

group = rootProject.group
version = rootProject.version

android {
    namespace = "se.warting.signatureink"
    compileSdk = 37
    defaultConfig { minSdk = 23 }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    lint {
        checkReleaseBuilds = true
        checkAllWarnings = true
        warningsAsErrors = true
        abortOnError = true
        disable.addAll(listOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "OldTargetApi"))
        checkDependencies = true
        checkGeneratedSources = false
    }
}

dependencies {
    constraints {
        implementation(libs.androidx.lifecycle.lifecycle.runtime.ktx) {
            because("Align Ink's transitive Lifecycle lint checks with the repository's AGP-compatible version")
        }
    }
    detektPlugins(libs.io.gitlab.arturbosch.detekt.detekt.formatting)
    api(libs.androidx.ink.brush)
    api(libs.androidx.ink.strokes)
    implementation(libs.androidx.ink.authoring)
    implementation(libs.androidx.ink.rendering)
    implementation(libs.androidx.input.motionprediction)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.runtime)
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

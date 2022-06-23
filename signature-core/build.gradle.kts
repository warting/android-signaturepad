plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.6.21"
    id("com.gladed.androidgitversion") version "0.4.14"
}

androidGitVersion {
    tagPattern = "^v[0-9]+.*"
}

val PUBLISH_GROUP_ID: String by extra("se.warting.signature")
val PUBLISH_VERSION: String by extra(androidGitVersion.name().replace("v", ""))
val PUBLISH_ARTIFACT_ID by extra("signature-core")

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")

val composeVersion = "1.2.0-rc02"
android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = false
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOfNotNull(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xallow-jvm-ir-dependencies",
                "-Xskip-prerelease-check"
        )
    }
    lint {
        baseline = file("lint-baseline.xml")
        checkReleaseBuilds = true
        checkAllWarnings = true
        warningsAsErrors = true
        abortOnError = true
        disable.add("LintBaseline")
        disable.add("GradleDependency")
        checkDependencies = true
        checkGeneratedSources = false
        sarifOutput = file("../lint-results-signature-core.sarif")
    }
}

dependencies {
    val composeVersion = "1.0.5"
    implementation("androidx.compose.runtime:runtime:$composeVersion")
}

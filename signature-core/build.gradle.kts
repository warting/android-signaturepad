plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.8.20"
    id("com.gladed.androidgitversion") version "0.4.14"
}

androidGitVersion {
    tagPattern = "^v[0-9]+.*"
}

val PUBLISH_GROUP_ID: String by extra("se.warting.signature")
val PUBLISH_VERSION: String by extra(androidGitVersion.name().replace("v", ""))
val PUBLISH_ARTIFACT_ID by extra("signature-core")

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")


android {
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = false
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.4"
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        freeCompilerArgs = listOfNotNull(
                "-opt-in=kotlin.RequiresOptIn",
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
    namespace = "se.warting.signaturecore"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


        dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2023.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.runtime:runtime")
}

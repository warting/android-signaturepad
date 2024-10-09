import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.gladed.androidgitversion") version "0.4.14"
    id("kotlin-kapt")
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
    alias(libs.plugins.com.vanniktech.maven.publish)
}

detekt {
    autoCorrect = true
    buildUponDefaultConfig = true
}

androidGitVersion {
    tagPattern = "^v[0-9]+.*"
}

val PUBLISH_GROUP_ID: String by extra(rootProject.group as String)
val PUBLISH_VERSION: String by extra(rootProject.version as String)

group = PUBLISH_GROUP_ID
version = PUBLISH_VERSION

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false

            buildConfigField("int", "VERSION_CODE", androidGitVersion.code().toString())
            buildConfigField("String", "VERSION_NAME", "\"${androidGitVersion.name()}\"")
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("int", "VERSION_CODE", androidGitVersion.code().toString())
            buildConfigField("String", "VERSION_NAME", "\"${androidGitVersion.name()}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = false
        compose = false
        buildConfig = true
    }
    dataBinding {
        enable = true
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
        sarifOutput = file("../lint-results-signature-view.sarif")
    }
    namespace = "se.warting.signatureview"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    api(project(":signature-core"))
    implementation(libs.androidx.core.core.ktx)
    detektPlugins(libs.io.gitlab.arturbosch.detekt.detekt.formatting)
}
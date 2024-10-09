import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("maven-publish")
    id("signing")
    alias(libs.plugins.org.jetbrains.dokka)
    alias(libs.plugins.com.gladed.androidgitversion)
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
    alias(libs.plugins.compose.compiler)
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
    val composeBom = platform(libs.androidx.compose.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.runtime)
    detektPlugins(libs.io.gitlab.arturbosch.detekt.detekt.formatting)
}

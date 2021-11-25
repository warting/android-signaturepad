plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.6.0"
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
    compileSdk = 31

    defaultConfig {
        minSdk = 14
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
        kotlinCompilerExtensionVersion = "1.0.3"
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
        baseline(file("lint-baseline.xml"))
        isCheckReleaseBuilds = true
        isCheckAllWarnings = true
        isWarningsAsErrors = true
        isAbortOnError = true
        disable.add("LintBaseline")
        disable.add("GradleDependency")
        isCheckDependencies = true
        isCheckGeneratedSources = false
        sarifOutput = file("../lint-results-signature-core.sarif")
    }
}

dependencies {
    val composeVersion = "1.0.5"
    implementation("androidx.compose.runtime:runtime:$composeVersion")
}

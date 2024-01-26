// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")

        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.0")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.47.0"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
}

apiValidation {
    ignoredProjects.add("app")
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.0")
    }

    detekt {
        autoCorrect = true
    }

    // https://github.com/otormaigh/playground-android/issues/27
    repositories {
        google()
        mavenCentral()
    }
}

detekt {
    autoCorrect = true
    buildUponDefaultConfig = true
    config = files("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")

    reports {
        html.enabled = true
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}


task<Delete>("clean") {
    delete(rootProject.buildDir)
}

apply(from = "${rootDir}/gradle/publish-root.gradle")

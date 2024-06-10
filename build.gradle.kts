// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.io.FileInputStream
import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.com.android.tools.build.gradle)
        classpath(libs.org.jetbrains.kotlin.kotlin.gradle.plugin)
        classpath(libs.io.gitlab.arturbosch.detekt.detekt.gradle.plugin)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

plugins {
    alias(libs.plugins.com.github.ben.manes.versions)
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
    alias(libs.plugins.io.github.gradle.nexus.publish.plugin)
    alias(libs.plugins.org.jetbrains.kotlinx.binary.compatibility.validator)
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    alias(libs.plugins.com.android.application).apply(false)
    alias(libs.plugins.com.android.library).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    id("com.google.devtools.ksp") version "2.0.0-1.0.22" apply false
    alias(libs.plugins.com.gladed.androidgitversion)
    alias(libs.plugins.androidx.room).apply(false)
}

androidGitVersion {
    tagPattern = "^v[0-9]+.*"
}


val localPropertiesFile: File = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val gitOrLocalVersion: String =
    localProperties.getProperty("VERSION_NAME") ?: androidGitVersion.name().replace("v", "")

version = gitOrLocalVersion
group = "se.warting.signature"


versionCatalogUpdate {
    sortByKey.set(true)
    keep {
        // keep versions without any library or plugin reference
        keepUnusedVersions.set(false)
        // keep all libraries that aren't used in the project
        keepUnusedLibraries.set(false)
        // keep all plugins that aren't used in the project
        keepUnusedPlugins.set(false)
    }
}

apiValidation {
    ignoredProjects.add("app")
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

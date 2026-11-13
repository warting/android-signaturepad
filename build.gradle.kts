// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Pin the Kotlin compiler used by AGP's built-in support to the same
        // version as the Compose and Parcelize compiler plugins.
        classpath(libs.org.jetbrains.kotlin.kotlin.gradle.plugin)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

plugins {
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
    alias(libs.plugins.io.github.gradle.nexus.publish.plugin)
    alias(libs.plugins.org.jetbrains.kotlinx.binary.compatibility.validator)
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    alias(libs.plugins.com.android.application).apply(false)
    alias(libs.plugins.com.android.library).apply(false)
    alias(libs.plugins.com.android.legacy.kapt).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.kotlin.parcelize).apply(false)
}

val localPropertiesFile: File = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use(localProperties::load)
}

// Native Git supports both regular clones and linked worktrees. Keep release
// versions tag-based and retain the existing MMMNNNPPP signature version codes.
val gitVersionName = providers.exec {
    commandLine("git", "describe", "--tags", "--always", "--dirty", "--match", "v[0-9]*")
}.standardOutput.asText.get().trim()
val gitVersionTag = providers.exec {
    commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
    isIgnoreExitValue = true
}.standardOutput.asText.get().trim()
val versionParts = Regex("^v(\\d+)\\.(\\d+)\\.(\\d+)").find(gitVersionTag)
extra.set(
    "libraryVersionCode",
    versionParts?.destructured?.let { (major, minor, patch) ->
        major.toInt() * 1_000_000 + minor.toInt() * 1_000 + patch.toInt()
    } ?: 0
)
extra.set("libraryVersionName", gitVersionName)

version = providers.gradleProperty("VERSION_NAME").orNull
    ?: localProperties.getProperty("VERSION_NAME")
    ?: gitVersionName.removePrefix("v")
group = "se.warting.signature"


versionCatalogUpdate {
    sortByKey.set(true)
    keep {
        // keep versions without any library or plugin reference
        keepUnusedVersions.set(false)
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

subprojects {
    fun configureAndroidKotlin() {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
            jvmToolchain(21)
            compilerOptions {
                freeCompilerArgs.addAll(
                    "-opt-in=kotlin.RequiresOptIn",
                )
            }
        }
        extensions.configure<com.android.build.api.dsl.CommonExtension> {
            compileOptions.sourceCompatibility = JavaVersion.VERSION_21
            compileOptions.targetCompatibility = JavaVersion.VERSION_21
        }
    }
    plugins.withId("com.android.application") { configureAndroidKotlin() }
    plugins.withId("com.android.library") {
        configureAndroidKotlin()

        // BCV 0.18.x only auto-registers tasks for the old kotlin-android plugin.
        // Keep the existing API snapshots and check/dump commands with built-in Kotlin.
        dependencies.add("bcv-rt-jvm-cp", libs.org.jetbrains.kotlin.kotlin.metadata.jvm)
        val apiBuild = tasks.register<kotlinx.validation.KotlinApiBuildTask>("apiBuild") {
            inputClassesDirs.from(
                tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileReleaseKotlin")
                    .flatMap { it.destinationDirectory },
                tasks.named<JavaCompile>("compileReleaseJavaWithJavac")
                    .flatMap { it.destinationDirectory },
            )
            runtimeClasspath.from(configurations.named("bcv-rt-jvm-cp-resolver"))
            outputApiFile.set(layout.buildDirectory.file("api/${project.name}.api"))
        }
        val apiCheck = tasks.register<kotlinx.validation.KotlinApiCompareTask>("apiCheck") {
            group = "verification"
            description = "Checks the library's public API against its committed snapshot."
            projectApiFile.set(layout.projectDirectory.file("api/${project.name}.api"))
            generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
        }
        tasks.register<Copy>("apiDump") {
            group = "other"
            description = "Updates the library's public API snapshot."
            from(apiBuild.flatMap { it.outputApiFile })
            into(layout.projectDirectory.dir("api"))
        }
        tasks.named("check") { dependsOn(apiCheck) }
    }
}

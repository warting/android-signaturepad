plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
    alias(libs.plugins.com.vanniktech.maven.publish)
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(21)
    jvm()
    iosArm64()
    iosSimulatorArm64()
    wasmJs {
        browser { testTask { useKarma { useChromeHeadless() } } }
        nodejs()
    }
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

mavenPublishing {

    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("signature-engine")
        description.set("Portable signature model, gesture handling, curve smoothing and SVG export")
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

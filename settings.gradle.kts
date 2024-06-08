pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

develocity{
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        publishing.onlyIf { false }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.develocity") version "3.17.4"
}

buildCache {
    local {
        isEnabled = true
    }
    remote<HttpBuildCache> {
        isEnabled = false
    }
}


rootProject.name = "Signaturepad"
include(":app")
include(":signature-pad")
include(":signature-view")
include(":signature-core")

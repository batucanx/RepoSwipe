pluginManagement {
    repositories {
        google()
        // GCS-backed Maven Central mirror. Some ISPs SNI-block the Cloudflare-fronted
        // repo.maven.apache.org / plugins.gradle.org; this Google-hosted mirror routes
        // around that and carries the same artifacts (including plugin markers).
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "RepoSwipe"

include(":app")

// Macrobenchmark + Baseline Profile generator. Not shipped — com.android.test module.
include(":baselineprofile")

include(":core:common")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:data")

include(":feature:auth")
include(":feature:swipe")
include(":feature:filter")
include(":feature:leaderboard")
include(":feature:starred")
include(":feature:profile")
include(":feature:settings")
include(":feature:followers")

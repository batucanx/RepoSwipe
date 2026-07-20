pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RepoSwipe"

include(":app")

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

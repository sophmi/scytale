pluginManagement {
    includeBuild("./gradle/convention-plugins")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

rootProject.name = "scytale"

include(
    ":whirlpool"
)

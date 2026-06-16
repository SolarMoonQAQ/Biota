rootProject.name = "[Biota]1.21.1-neoforge"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { setUrl("https://maven.neoforged.net/releases") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
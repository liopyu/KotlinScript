enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://api.modrinth.com/maven")

    }

    includeBuild("gradle/build-logic")
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.9.0")
}
rootProject.name = "kotlinscript"
include("common")
include("fabric")
include("neoforge")


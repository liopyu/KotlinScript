enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://api.modrinth.com/maven")
    }

    includeBuild("gradle/build-logic")
}

rootProject.name = "kotlinscript"
include("common")
include("fabric")
include("neoforge")


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
    }

    includeBuild("gradle/build-logic")
}

rootProject.name = "kotlinscript"
include("common")
include("fabric")
include("neoforge")


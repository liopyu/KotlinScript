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

listOf(
    "common",
    "fabric",
    "neoforge"
).forEach { setupProject(it, file(it)) }

fun setupProject(name: String, projectDirectory: File) = setupProject(name) {
    projectDir = projectDirectory
}

inline fun setupProject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    id("common")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.spongepowered.gradle.plugin") version "2.2.0"
}

version = "0.1.6"

sponge {
    apiVersion("8.2.0-SNAPSHOT")
    license("MIT")
    loader {
        name("java_plain")
        version("1.0")
    }
    plugin("scripting-host") {
        displayName("scripting-host")
        entrypoint("me.zodd.Host")
        description("A host plugin for running Kotlin Scripts")
    }
}

dependencies {
    implementation("org.spongepowered:configurate-extra-kotlin:4.1.2") {
        isTransitive = false
    }
    shadow(kotlin("reflect"))
    shadow(project(":script-definition"))
    shadow(kotlin("scripting-jvm-host"))
    shadow(project(":api"))
    api("net.kyori:adventure-api:4.14.0")
}

tasks.jar.get().enabled = false

tasks.shadowJar {
    archiveClassifier.set("")
    configurations.add(project.configurations.shadow.get())
    listOf(
        "org.spongepowered.configurate.kotlin", "org.jetbrains.org.objectweb.asm",
        "org.jetbrains.jps", "javaslang", "gnu.trove", "com.sun.jna", "messages", "misc"
    ).forEach { relocate(it, "me.zodd.shaded.$it") }
    mergeServiceFiles()
}
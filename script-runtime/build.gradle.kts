plugins {
    id("common")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    shadow(project(":script-definition"))
    shadow(kotlin("script-runtime"))
    shadow(project(":api"))
    shadow("org.spongepowered:spongeapi:8.1.0")
    shadow("net.kyori:adventure-api:4.14.0")
}

tasks.jar {
    enabled = false
    finalizedBy("shadowJar")
}

tasks.shadowJar {
    archiveClassifier.set("")
    configurations.add(project.configurations.shadow.get())
}
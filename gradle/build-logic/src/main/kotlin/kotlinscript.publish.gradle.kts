import utilities.VersionType
import utilities.writeVersion

plugins {
    id("java")
    id("java-library")
    id("maven-publish")
    id("dev.architectury.loom")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven("https://maven.impactdev.net/repository/development/") {
            name = "ImpactDev-Public"
            credentials {
                username = System.getenv("KOTLINSCRIPT_MAVEN_USER")
                password = System.getenv("KOTLINSCRIPT_MAVEN_PASSWORD")
            }
        }
    }

    publications {
        create<MavenPublication>(project.name) {
            artifact(tasks.remapJar)
            artifact(tasks.remapSourcesJar)

            @Suppress("UnstableApiUsage")
            loom.disableDeprecatedPomGeneration(this)

            groupId = "net.liopyu"
            artifactId = project.findProperty("maven.artifactId")?.toString() ?: project.name
            version = project.writeVersion(VersionType.PUBLISHING)
        }
    }
}

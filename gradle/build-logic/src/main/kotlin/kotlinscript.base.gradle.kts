

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import utilities.ACCESS_WIDENER

plugins {
    id("java")
    id("java-library")

    id("dev.architectury.loom")
    id("architectury-plugin")
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version
description = rootProject.description

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    //JEI
    maven("https://maven.blamejared.com/")
    maven("https://maven.parchmentmc.org")
}

architectury {
    minecraft = project.property("mc_version").toString()
}

loom {
    silentMojangMappingsLicense()
    accessWidenerPath.set(project(":common").file(ACCESS_WIDENER))
}

dependencies {
    minecraft("net.minecraft:minecraft:${rootProject.property("mc_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.21:${rootProject.property("parchment_version")}")
    })
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.add("-Xlint:-processing,-classfile,-serial")
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    withType<Jar> {
        from(rootProject.file("LICENSE"))
    }
}

plugins {
    //id("common")
    id("eclipse")
    id("idea")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecraftforge.gradle") version "[6.0.16,6.2)"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id ("org.jetbrains.kotlin.jvm") version ("1.8.22")
    id ("org.jetbrains.kotlin.plugin.serialization") version ("1.8.22")
    //`kotlin-dsl`

}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
minecraft {
    mappings("official", "1.20.1")
    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")
        }
        create("server") {
            workingDirectory(project.file("run/server"))
            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")
        }
    }
    // accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://cursemaven.com") {
        content { includeGroup("curse.maven") }
    }
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "Kotlin for Forge"
    }
    maven(url="https://repo.nyon.dev/releases")
}
dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.2.20")
    implementation("dev.nyon:KotlinLangForge:1.0.0-k2.0.20-1.20.1+forge")
    /*implementation("org.spongepowered:configurate-extra-kotlin:4.1.2") {
        isTransitive = false
    }*/
    shadow(kotlin("reflect"))
    //shadow(project(":script-definition"))
    api(kotlin("scripting-jvm"))
    shadow(kotlin("scripting-jvm-host"))
    //shadow(project(":api"))
    //api("net.kyori:adventure-api:4.14.0")

    //implementation("thedarkcolour:kotlinforforge:4.10.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    configurations.add(project.configurations.shadow.get())

    listOf(
        "org.spongepowered.configurate.kotlin",
        "org.jetbrains.org.objectweb.asm",  // Used by Kotlin internally
        "org.jetbrains.jps",
        "javaslang",
        "gnu.trove",
        "com.sun.jna",
        "messages",
        "misc"
    ).forEach { relocate(it, "me.zodd.shaded.$it") }

    mergeServiceFiles()
}

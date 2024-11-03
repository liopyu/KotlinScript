import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecraftforge.gradle") version "[6.0.16,6.2)"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    kotlin("jvm") version "1.8.22"
    id ("org.spongepowered.mixin") version ("0.7.+")
    id ("org.jetbrains.kotlin.plugin.serialization") version ("1.8.0")
}

group = "me.zodd"
version = "0.1.7"

val mixinVersion: String by project

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}
minecraft {
    mappings("official", "1.20.1")
    runs {
        create("client") {
            workingDirectory(project.file("run"))
            args ("--mixin.config=kotlinscript.mixins.json", "--debug")
            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")
            property ("mixin.env.remapRefMap", "true")
            property ("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
        }
        create("server") {
            workingDirectory(project.file("run/server"))
            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")
            property ("mixin.env.remapRefMap", "true")
            property ("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
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
val shadeKotlin by configurations.creating
val library = configurations.create("library")
configurations {
    library.extendsFrom(this["shadow"])
    implementation.get().extendsFrom(library)
    compileOnly.get().extendsFrom(shadeKotlin)
}
fun Jar.createManifest() = manifest {
    attributes(
        "Automatic-Module-Name" to "kotlinscript",
        "Specification-Title" to "kotlinscript",
        "Specification-Vendor" to "liopyu",
        "Specification-Version" to "1",
        "Implementation-Title" to project.name,
        "Implementation-Version" to version,
        "Implementation-Vendor" to "liopyu",
        "Implementation-Timestamp" to ZonedDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")),
        "MixinConfigs" to "kotlinscript.mixins.json"
    )
}
dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.2.20")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.2.1")?.let { implementation(it) }
    annotationProcessor("org.spongepowered:mixin:${mixinVersion}:processor")
    //Shadows
    shadow(kotlin("reflect"))

    shadow("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3")
    shadow("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3")
    shadow("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")
    shadow("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    shadow(kotlin("scripting-jvm"))
    shadow(kotlin("scripting-jvm-host"))
    //implementation("thedarkcolour:kotlinforforge:4.10.0")
    shadow("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    shadow("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")
    /*

    // Implementations
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-jvm-host"))
    //implementation("thedarkcolour:kotlinforforge:4.10.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")*/


}
val userConfig = Properties()
if (System.getProperty("user.name").equals(userConfig.getProperty("user"))) {
    tasks.getByName("shadowJar").finalizedBy("copyJar")
}
tasks.jar {
    enabled = false
    finalizedBy("reobfJar")
}
val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations = listOf(library, shadeKotlin)

    exclude(
        "LICENSE.txt", "META-INF/MANIFSET.MF", "META-INF/maven/**",
        "META-INF/*.RSA", "META-INF/*.SF", "META-INF/versions/**"
    )

    dependencies {
        exclude(dependency("net.java.dev.jna:jna"))
    }

    relocate("org.jetbrains.kotlin.fir.analysis.native", "org.jetbrains.kotlin.fir.analysis.notnative")
    relocate(
        "org.jetbrains.kotlin.fir.analysis.diagnostics.native",
        "org.jetbrains.kotlin.fir.analysis.diagnostics.notnative"
    )

    val packages = listOf(
        "gnu.trove"
    )

    packages.forEach { relocate(it, "me.zodd.repack.$it") }

    exclude("**/module-info.class")

    createManifest()

    //finalizedBy("reobfShadowJar")
}
/*
tasks.shadowJar {
    archiveClassifier.set("")
    configurations.add(project.configurations.shadow.get())
    listOf(
         "org.jetbrains.org.objectweb.asm",
        "org.jetbrains.jps", "javaslang", "gnu.trove", "com.sun.jna", "messages", "misc"
    ).forEach { relocate(it, "me.zodd.shaded.$it") }
    mergeServiceFiles()
}*/

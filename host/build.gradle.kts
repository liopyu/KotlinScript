import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.userdev.tasks.JarJar

plugins {
    java
    eclipse
    idea
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
    //id("common")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecraftforge.gradle") version "[6.0.16,6.2)"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"

}

version = "0.1.6"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
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
val customJarFolder = file("jarjarlibs")
dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.2.20")
    //implementation("thedarkcolour:kotlinforforge:4.10.0")
    /*shadow(kotlin("scripting-jvm"))
    shadow(kotlin("scripting-jvm-host"))
    shadow(kotlin("scripting-common"))
    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-jvm-host"))
    implementation(kotlin("scripting-common"))*/
//Shadows
    //implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
     shadow ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
     implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    /* jarJar("org.jetbrains.kotlin", "kotlin-stdlib", "[1.8.10,)")
    jarJar("org.jetbrains.kotlin", "kotlin-reflect", "[1.8.10,)")
   */ // shadow ("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
     shadow ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")
     shadow ("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3")
     shadow ("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3")
     shadow( "org.jetbrains.kotlin:kotlin-scripting-jsr223:1.8.0")

     shadow ("org.jetbrains.kotlin:kotlin-scripting-common:1.8.0")
    shadow ("org.jetbrains.kotlin:kotlin-scripting-jvm:1.8.10")
    shadow ("org.jetbrains.kotlin:kotlin-script-runtime:1.8.0")
    shadow ("org.jetbrains.kotlin:kotlin-script-util:1.8.0")
    shadow ("org.jetbrains.kotlin:kotlin-scripting-jvm-host:1.8.0")
    shadow ("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.8.0")
    shadow ("org.jetbrains.kotlin:kotlin-scripting-compiler-impl:1.8.0")
    shadow ("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.8.0")
    shadow ("org.jetbrains.intellij.deps:trove4j:1.0.20200330")
    shadow ("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:1.8.0")

    // shadow "org.jetbrains.kotlin:kotlin-main-kts:1.8.0"
}
jarJar.enable()
tasks.jarJar {
    archiveClassifier.set("jarjar")
}
tasks.register<Jar>("zcombinedJar") {
    //duplicatesStrategy = DuplicatesStrategy.INHERIT
    archiveClassifier.set("all")
    from(zipTree(tasks.named<JarJar>("jarJar").get().archiveFile)) {
        include("META-INF/jarjar/*.jar")
    }
    from(customJarFolder) {
        include("**")
        into("META-INF/jarjar")
    }
    from(zipTree(tasks.named<ShadowJar>("shadowJar").get().archiveFile))
    dependsOn(/*tasks.named("jarJar"),*/ tasks.named("shadowJar"))
}
tasks.named<ShadowJar>("shadowJar") {

   dependsOn(tasks.jarJar)
    dependencies {
        //include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        //include(dependency("org.jetbrains.kotlin:kotlin-reflect"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
        include(dependency("org.jetbrains.kotlin:kotlin-scripting-jsr223"))
        include(dependency("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm"))
        include(dependency("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm"))

        include(dependency("org.jetbrains.kotlin:kotlin-scripting-common"))
        include(dependency("org.jetbrains.kotlin:kotlin-scripting-jvm-host"))
        include(dependency("org.jetbrains.kotlin:kotlin-scripting-jvm"))
        include(dependency("org.jetbrains.kotlin:kotlin-script-runtime"))
        include(dependency("org.jetbrains.kotlin:kotlin-script-util"))
        include(dependency("org.jetbrains.kotlin:kotlin-compiler-embeddable"))
        include(dependency("org.jetbrains.kotlin:kotlin-scripting-compiler-impl"))
        include(dependency("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable"))
        include(dependency("org.jetbrains.intellij.deps:trove4j"))
        include(dependency("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable"))
        include(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core"))
    }
    archiveClassifier.set("")
    configurations.add(project.configurations.shadow.get())
    listOf(
        /*"kotlin",*/
        "org.spongepowered.configurate.kotlin", "org.jetbrains.org.objectweb.asm",
        "org.jetbrains.jps", "javaslang", "com.sun.jna", "messages", "misc"
    ).forEach { relocate(it, "me.zodd.shaded.$it") }
    mergeServiceFiles()
    minimize()
}
tasks.jar.get().enabled = false

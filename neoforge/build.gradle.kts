plugins {
    id("kotlinscript.platform")
    id("kotlinscript.publish")
    id("com.gradleup.shadow") version "8.3.0"
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    neoForge {

    }
}
kotlin {
    jvmToolchain(21)
}
repositories {
    maven(url = "${rootProject.projectDir}/deps")
    maven(url = "https://thedarkcolour.github.io/KotlinForForge/")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.neoforged.net/releases")
    mavenLocal()
}

val relocatePairs = listOf(
    "org.jetbrains.kotlin.backend.native" to "net.liopyu.kotlinscript.shadow.kotlinx.backend.kn",
    "org.jetbrains.kotlin.ir.backend.native" to "net.liopyu.kotlinscript.shadow.kotlinx.ir.backend.kn",
    "org.jetbrains.kotlin.fir.backend.native" to "net.liopyu.kotlinscript.shadow.kotlinx.fir.backend.kn",
    "org.jetbrains.kotlin.fir.analysis.native" to "net.liopyu.kotlinscript.shadow.kotlinx.fir.analysis.kn",
    "org.jetbrains.kotlin.fir.analysis.diagnostics.native" to "net.liopyu.kotlinscript.shadow.kotlinx.fir.analysis.diagnostics.kn",
    "org.jetbrains.kotlin.fir.native" to "net.liopyu.kotlinscript.shadow.kotlinx.fir.kn",
    "org.jetbrains.kotlin.analysis.native" to "net.liopyu.kotlinscript.shadow.kotlinx.analysis.kn",
    "org.jetbrains.kotlin.resolve.native" to "net.liopyu.kotlinscript.shadow.kotlinx.resolve.kn",
    "org.jetbrains.kotlin.incremental.native" to "net.liopyu.kotlinscript.shadow.kotlinx.incremental.kn",
    "org.jetbrains.kotlin.native.interop" to "net.liopyu.kotlinscript.shadow.kotlinx.kn.interop",
    "org.jetbrains.kotlin.native" to "net.liopyu.kotlinscript.shadow.kotlinx.kn",
    "kotlin.native" to "net.liopyu.kotlinscript.shadow.kotlinx.kn"
)
val compilerJiJ by configurations.creating
dependencies {
    neoForge(libs.neoforge)

    implementation(project(":common", configuration = "namedElements")) { isTransitive = false }
    "developmentNeoForge"(project(":common", configuration = "namedElements")) { isTransitive = false }
    bundle(project(path = ":common", configuration = "transformProductionNeoForge")) { isTransitive = false }

    bundle("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    bundle("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    // >>> move compiler artifacts to the shaded bundle:
    bundle("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    bundle("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:2.0.21")
    bundle("org.jetbrains.kotlin:kotlin-scripting-jvm:2.0.21")
    bundle("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.0.21")

    // remove any include()/runtimeOnly() for those four — no more JIJ for them

    bundle("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    bundle("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    bundle("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    bundle("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.9.0")


    bundle("io.github.classgraph:classgraph:4.8.149")
}

listOf("bundle", "runtimeClasspath", "compileClasspath").forEach { cfg ->
    configurations.named(cfg) {
        exclude(group = "com.google.guava")
        exclude(group = "io.netty")
        exclude(group = "com.mojang", module = "brigadier")
        exclude(group = "com.google.code.gson")
        exclude(group = "com.mojang", module = "authlib")
        exclude(group = "com.mojang", module = "datafixerupper")
        exclude(group = "it.unimi.dsi")
        exclude(group = "org.lwjgl")
        exclude(group = "net.java.dev.jna")
        exclude(group = "org.checkerframework")
        exclude(group = "com.google.j2objc")
        exclude(group = "com.google.errorprone")
        exclude(group = "com.google.code.findbugs")

        exclude(group = "com.jcraft")            // jorbis/jogg
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "org.apache.commons")     // commons-io, commons-lang3, etc.
        exclude(group = "com.github.oshi")        // oshi-core
    }
}


configurations.named("bundle") {
    exclude(group = "com.google.guava", module = "failureaccess")
    exclude(group = "com.google.guava", module = "guava")
    exclude(group = "com.google.guava", module = "listenablefuture")
}

tasks {
    shadowJar {
        configurations = listOf(project.configurations["bundle"])
        isZip64 = true
        exclude("architectury-common.accessWidener")
        exclude("architectury.common.json")
        mergeServiceFiles()
        relocate("com.ibm.icu", "net.liopyu.kotlinscript.ibm.icu")
        relocatePairs.forEach { (from, to) -> relocate(from, to) }
    }
    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
    }
    processResources {
        inputs.property("version", rootProject.version)
        inputs.property("minecraft_version", rootProject.property("mc_version").toString())
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(
                "version" to rootProject.version,
                "minecraft_version" to rootProject.property("mc_version").toString()
            )
        }
    }
    sourcesJar {
        val depSources = project(":common").tasks.sourcesJar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(depSources)
        from(depSources.get().archiveFile.map { zipTree(it) }) {
            exclude("architectury.accessWidener")
        }
    }
}

tasks {
    sourcesJar {
        val depSources = project(":common").tasks.sourcesJar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(depSources)
        from(depSources.get().archiveFile.map { zipTree(it) }) {
            exclude("architectury.accessWidener")
        }
    }
}

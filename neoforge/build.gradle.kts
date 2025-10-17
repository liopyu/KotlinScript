plugins {
    id("kotlinscript.platform")
    id("kotlinscript.publish")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom { neoForge { } }

kotlin { jvmToolchain(21) }

repositories {
    maven(url = "${rootProject.projectDir}/deps")
    maven(url = "https://thedarkcolour.github.io/KotlinForForge/")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.neoforged.net/releases")
    mavenLocal()
}

val kotlinVersion = "2.0.21"

val embeddedLibs by configurations.creating
val commonJarInput by configurations.creating

dependencies {
    neoForge(libs.neoforge)
    implementation(libs.neo.kotlin.forge)

    implementation(project(":common", configuration = "namedElements")) { isTransitive = false }
    "developmentNeoForge"(project(":common", configuration = "namedElements")) { isTransitive = false }

    add("commonJarInput", project(path = ":common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }

    bundle(project(path = ":common", configuration = "transformProductionNeoForge")) { isTransitive = false }

    listOf(
        "org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion",
        "org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion",
        "org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion",
        "org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion",
        "org.jetbrains.kotlin:kotlin-scripting-dependencies",
        "org.jetbrains.kotlin:kotlin-scripting-dependencies-maven",
        "io.github.classgraph:classgraph:4.8.149",
        kotlin("script-runtime"),
        libs.graal
    ).forEach {
        val dep = when (it) {
            is Provider<*> -> it
            else -> dependencies.create(it.toString()).also { d ->
                (d as? ExternalModuleDependency)?.isTransitive = false
            }
        }
        add(embeddedLibs.name, dep)
    }
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes["FMLModType"] = "MOD" }

        from({ commonJarInput.resolve().map { zipTree(it) } }) {
            exclude("architectury.accessWidener", "architectury.common.json")
        }

        from({ embeddedLibs.resolve().map { zipTree(it) } }) {
            exclude(
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/INDEX.LIST",
                "module-info.class"
            )
            includeEmptyDirs = false
            eachFile {
                val dot = path.replace('/', '.')
                val drop =
                    dot.startsWith("org.jetbrains.kotlin.native.") ||
                            dot.startsWith("org.jetbrains.kotlin.konan.") ||
                            dot.startsWith("org.jetbrains.kotlin.cli.konan.") ||
                            dot.startsWith("org.jetbrains.kotlin.resolve.native.") ||
                            dot.startsWith("org.jetbrains.kotlin.incremental.native.") ||
                            dot.contains(".native.")
                if (drop) exclude()
            }
        }
    }

    remapJar {
        dependsOn(jar)
        inputFile.set(jar.flatMap { it.archiveFile })
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

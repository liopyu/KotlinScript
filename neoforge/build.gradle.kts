plugins {
    id("kotlinscript.platform")
    id("kotlinscript.publish")
    id("net.neoforged.moddev") version "2.0.78"
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

dependencies {
    neoForge(libs.neoforge)
    implementation(project(":common", configuration = "namedElements")) { isTransitive = false }
    "developmentNeoForge"(project(":common", configuration = "namedElements")) { isTransitive = false }
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion")
}

listOf("runtimeClasspath", "compileClasspath").forEach { cfg ->
    configurations.named(cfg) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-debug")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
}

configurations.named("jarJar") {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-debug")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
}

val kotlinEmbeds = listOf(
    "org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion",
    "org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion",
    "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion",
    "org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:$kotlinVersion",
    "org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion",
    "org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion"
)

val sanitizedDir = layout.buildDirectory.dir("jarjar-sanitized")

val sanitizedJars = kotlinEmbeds.map { gav ->
    val parts = gav.split(":")
    val module = parts[1]
    val ver = parts[2]
    val taskName = "sanitize_${module}_${ver}".replace('-', '_').replace('.', '_')
    val dep = dependencies.create(gav) as ExternalModuleDependency
    dep.isTransitive = false
    val cfg = configurations.detachedConfiguration(dep).apply { isTransitive = false }
    tasks.register<Jar>(taskName) {
        val inFile = providers.provider { cfg.resolve().single() }
        from(inFile.map { zipTree(it) })
        exclude(
            "org/jetbrains/kotlin/native/**",
            "org/jetbrains/kotlin/konan/**",
            "org/jetbrains/kotlin/cli/konan/**",
            "org/jetbrains/kotlin/resolve/native/**",
            "org/jetbrains/kotlin/incremental/native/**",
            "org/jetbrains/kotlin/fir/**/native/**",
            "kotlin/native/**",
            "module-info.class"
        )
        archiveBaseName.set(module)
        archiveVersion.set(ver)
        destinationDirectory.set(sanitizedDir)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes("Automatic-Module-Name" to module.replace('-', '.') + ".sanitized") }
    }
}

sanitizedJars.forEach { t ->
    dependencies.add("jarJar", files(t.flatMap { it.archiveFile }))
}

val commonJarInput by configurations.creating
dependencies {
    add("commonJarInput", project(path = ":common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
}

tasks {
    val jarJarTask = getByName("jarJar")
    jar {
        dependsOn(jarJarTask)
        from(jarJarTask)
        from({ commonJarInput.resolve().map { zipTree(it) } })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude(
            "LICENSE",
            "LICENSE*",
            "NOTICE",
            "NOTICE*",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/INDEX.LIST",
            "module-info.class"
        )
        manifest {
            attributes["FMLModType"] = "MOD"
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

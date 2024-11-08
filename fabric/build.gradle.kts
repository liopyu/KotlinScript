

configurations.all {
    resolutionStrategy {
        force(libs.fabric.loader)
    }
}

plugins {
    id("kotlinscript.platform")
    id("kotlinscript.publish")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

val generatedResources = file("src/generated/resources")

sourceSets {
    main {
        resources {
            srcDir(generatedResources)
        }
    }
}

repositories {
    maven(url = "${rootProject.projectDir}/deps")
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://api.modrinth.com/maven")
}

dependencies {
    implementation(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    "developmentFabric"(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    bundle(project(path = ":common", configuration = "transformProductionFabric")) {
        isTransitive = false
    }
    modLocalRuntime(libs.fabric.debugutils)
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)
    modApi(libs.bundles.fabric)

    modCompileOnly(libs.bundles.fabric.integrations.compileOnly) {
        isTransitive = false
    }
    modRuntimeOnly(libs.bundles.fabric.integrations.runtimeOnly)
   /* include("org.jetbrains.kotlin:kotlin-scripting-jsr223:2.0.21")
    include("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    include("org.jetbrains.kotlin:kotlin-script-util:1.8.22")*/

    include("org.jetbrains.kotlin:kotlin-scripting-common:2.0.21")
    include("org.jetbrains.kotlin:kotlin-scripting-jvm:2.0.21")
    include("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.0.21")
   /* include("org.jetbrains.kotlin:kotlin-scripting-dependencies:2.0.21")
    include("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.0.21")*/

    implementation(kotlin("script-runtime"))
  /*  include(kotlin("script-runtime"))*/

   /* include(kotlin("scripting-jsr223"))
    include(kotlin("compiler-embeddable"))*/
    include(libs.fabric.kotlin)

    listOf(
/*
        kotlin("script-runtime"),
*/
        "org.jetbrains.kotlin:kotlin-scripting-common:2.0.21",
        "org.jetbrains.kotlin:kotlin-scripting-jvm:2.0.21",
        "org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.0.21",
      /*  "org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.0.21",
        "org.jetbrains.kotlin:kotlin-scripting-dependencies:2.0.21",*/
       /*
        kotlin("scripting-jsr223"),
        kotlin("compiler-embeddable"),*/
        libs.graal
    ).forEach {
        bundle(it)
        runtimeOnly(it)
    }

    minecraftServerLibraries(libs.icu4j)

}
tasks {
    // The AW file is needed in :fabric project resources when the game is run.
    val copyAccessWidener by registering(Copy::class) {
        from(loom.accessWidenerPath)
        into(generatedResources)
    }

    processResources {
        dependsOn(copyAccessWidener)
        inputs.property("version", rootProject.version)
        inputs.property("fabric_loader_version", libs.fabric.loader.get().version)
        inputs.property("minecraft_version", rootProject.property("mc_version").toString())

        filesMatching("fabric.mod.json") {
            expand(
                "version" to rootProject.version,
                "fabric_loader_version" to libs.fabric.loader.get().version,
                "minecraft_version" to rootProject.property("mc_version").toString()
            )
        }
    }

    sourcesJar {
        dependsOn(copyAccessWidener)
    }
}

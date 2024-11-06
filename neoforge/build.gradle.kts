

plugins {
    id("kotlinscript.platform")
    id("kotlinscript.publish")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    neoForge {

    }
}

repositories {
    maven(url = "${rootProject.projectDir}/deps")
    maven(url = "https://thedarkcolour.github.io/KotlinForForge/")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.neoforged.net/releases")
    mavenLocal()
}

dependencies {
    neoForge(libs.neoforge)
    //Because of the JEI mapping issues if we want
    //a forge launch we gotta do some wacky stuff
    //modImplementation(libs.jeiForge)
    //shadowCommon group: 'commons-io', name: 'commons-io', version: '2.6'
    //modImplementation(libs.flywheelForge)
    //include(libs.flywheelForge)
    modLocalRuntime(libs.neoforge.debugutils)
    implementation(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    implementation(libs.neo.kotlin.forge) {
        exclude(group = "net.neoforged.fancymodloader", module = "loader")
    }
    "developmentNeoForge"(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    bundle(project(path = ":common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
    testImplementation(project(":common", configuration = "namedElements"))

    listOf(
        libs.graal
    ).forEach {
        forgeRuntimeLibrary(it)
        bundle(it)
    }
}

tasks {
    shadowJar {
        exclude("architectury-common.accessWidener")
        exclude("architectury.common.json")

        relocate ("com.ibm.icu", "net.liopyu.relocations.ibm.icu")
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

//Stole from architect discord, replaces loom.forge.convertAccessWideners
tasks.remapJar {
    atAccessWideners.add("kotlin-common.accesswidener")
}


//jar {
//    classifier("dev")
//    manifest {
//        attributes(
//                "Specification-Title" to rootProject.mod_id,
//                "Specification-Vendor" to "Cable MC",
//                "Specification-Version" to "1",
//                "Implementation-Title" to rootProject.mod_id,
//                "Implementation-Version" to project.version,
//                "Implementation-Vendor" to "Cable MC",
//        )
//    }
//}
//
//sourcesJar {
//    def commonSources = project(":common").sourcesJar
//    dependsOn commonSources
//    from commonSources.archiveFile.map { zipTree(it) }
//}
//
//components.java {
//    withVariantsFromConfiguration(project.configurations.shadowRuntimeElements) {
//        skip()
//    }
//}
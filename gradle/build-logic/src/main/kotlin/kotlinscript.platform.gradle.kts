import utilities.VersionType
import utilities.writeVersion

plugins {
    id("kotlinscript.base")
    id("com.github.johnrengelman.shadow")
}

writeVersion(type = VersionType.FULL)

val bundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

loom {
    val clientConfig = runConfigs.getByName("client")
    clientConfig.runDir = "runClient"
    clientConfig.programArg("--username=liopyu")
    clientConfig.programArg("--uuid=102ef9be-d13e-496c-8a9d-6f07848324c8")
    val serverConfig = runConfigs.getByName("server")
    serverConfig.runDir = "runServer"
}

tasks {

    jar {
        archiveBaseName.set("KotlinScript-${project.name}")
        archiveClassifier.set("dev-slim")
    }

    shadowJar {
        archiveClassifier.set("dev-shadow")
        archiveBaseName.set("KotlinScript-${project.name}")
        configurations = listOf(bundle)
        mergeServiceFiles()

        relocate ("org.graalvm", "net.liopyu.relocations.graalvm")
        relocate ("com.oracle", "net.liopyu.relocations.oracle")
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
        archiveBaseName.set("KotlinScript-${project.name}")
        archiveVersion.set("${rootProject.version}")
    }

    val copyJar by registering(CopyFile::class) {
        val productionJar = tasks.remapJar.flatMap { it.archiveFile }
        fileToCopy = productionJar
        destination = productionJar.flatMap {
            rootProject.layout.buildDirectory.file("libs/${it.asFile.name}")
        }
    }

    assemble {
        dependsOn(copyJar)
    }

}
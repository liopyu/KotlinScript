import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import utilities.isSnapshot
import utilities.version



plugins {
    id("kotlinscript.base")
    id("kotlinscript.publish")

    id("net.kyori.blossom")
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("net.nemerosa.versioning") version "3.1.0"
}

architectury {
    common("neoforge", "fabric")
}

repositories {
    maven(url = "${rootProject.projectDir}/deps")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.neoforged.net/releases")
    mavenLocal()
}

dependencies {
    implementation(libs.bundles.kotlin)
    modImplementation(libs.fabric.loader)
    /*modApi(libs.molang)*/

    // Integrations
    compileOnlyApi(libs.jei.api)
    modCompileOnly(libs.bundles.fabric.integrations.compileOnly) {
        isTransitive = false
    }
    // Flywheel has no common dep so just pick one and don't use any platform specific code in common
    // modCompileOnly(libs.flywheelFabric)
    /*implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-script-util:1.8.22")*/

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.0.21")
   /* implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.0.21")*/
    // Showdown
    modCompileOnly(libs.graal)

    // Data Storage
    modCompileOnly(libs.bundles.mongo)

    // Unit Testing
    testImplementation(libs.bundles.unitTesting)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        setEvents(listOf("failed"))
        setExceptionFormat("full")
    }
}
/*
sourceSets {
    main {
        blossom {
            kotlinSources {
                fun generateLicenseHeader() : String {
                    val builder = StringBuilder()
                    builder.append("/*\n")
                    rootProject.file("HEADER").forEachLine {
                        if(it.isEmpty()) {
                            builder.append(" *").append("\n")
                        } else {
                            builder.append(" * ").append(it).append("\n")
                        }
                    }

                    return builder.append(" */").append("\n").toString()
                }

                property("license", generateLicenseHeader())
                property("modid", "kotlinscript")
                property("version", project.version())
                property("isSnapshot", if(rootProject.isSnapshot()) "true" else "false")
                property("gitCommit", versioning.info.commit)
                property("branch", versioning.info.branch)
                System.getProperty("buildNumber")?.let { property("buildNumber", it) }
                property("timestamp", OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss")) + " UTC")
            }
        }
    }
}*/
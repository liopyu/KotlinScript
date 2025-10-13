plugins {
    id("kotlinscript.base")
    id("kotlinscript.publish")

    id("net.kyori.blossom")
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("net.nemerosa.versioning") version "3.1.0"
}
kotlin {
    jvmToolchain(21)
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

    modCompileOnly(libs.fabric.loader)

    compileOnlyApi(libs.jei.api)
    modCompileOnly(libs.bundles.fabric.integrations.compileOnly) {
        isTransitive = false
    }

    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.0.21")

    modCompileOnly(libs.graal)
    modCompileOnly(libs.bundles.mongo)

    testImplementation(libs.bundles.unitTesting)
}


tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        setEvents(listOf("failed"))
        setExceptionFormat("full")
    }
}
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
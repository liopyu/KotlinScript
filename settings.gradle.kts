pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net")
        maven ("https://maven.parchmentmc.org")
    }
}

plugins {
    id ("org.gradle.toolchains.foojay-resolver-convention") version ("0.5.0")
}

rootProject.name = "KotlinScriptMod"
/*include("script-definition")*/
include("host")
/*include("api")
include("script-runtime")*/

package utilities

import org.gradle.api.Project

fun Project.version(): String {
    return rootProject.property("mod_version").toString()
}

fun Project.isSnapshot(): Boolean {
    return rootProject.property("snapshot") == "true"
}

fun Project.writeVersion(type: VersionType = VersionType.FULL): String {
    val version = "${rootProject.property("mod_version")}+${rootProject.property("mc_version")}"
    return when (type) {
        VersionType.PUBLISHING -> if(this.isSnapshot()) "$version-SNAPSHOT" else version
        VersionType.FULL -> if(this.isSnapshot()) rootProject.version.toString() else version
    }
}

enum class VersionType {
    PUBLISHING,
    FULL
}
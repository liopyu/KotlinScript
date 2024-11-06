plugins {
    id("kotlinscript.root")
    id ("net.nemerosa.versioning") version "3.1.0"
}

version = "${project.property("mod_version")}+${project.property("mc_version")}"

/*val isSnapshot = project.property("snapshot")?.equals("true") ?: false
if (isSnapshot) {
    val fixedBranchName = versioning.info.branch.substringAfter("/")
    version = "$version-${fixedBranchName}-${versioning.info.build}"
}*/

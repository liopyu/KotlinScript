plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
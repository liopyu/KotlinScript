plugins {
    id("common")
}

dependencies {
    compileOnly("org.spongepowered:spongeapi:8.1.0")
    compileOnly("net.kyori:adventure-api:4.14.0")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
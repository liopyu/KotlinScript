plugins {
    base
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}

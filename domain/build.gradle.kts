plugins {
    kotlin("plugin.spring") version "2.2.20"
}

dependencies {
    implementation("org.postgresql:postgresql")
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

tasks.test {
    enabled = false
}
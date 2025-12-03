plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("kapt")
}

springBoot {
    mainClass.set("com.project.message.MessageApplication")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-rsocket")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Kotlin 코루틴 기본
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    // Spring과 코루틴 통합
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")


}

tasks.test {
    enabled = false
}
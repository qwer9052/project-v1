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
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Kotlin 코루틴 기본
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    // Spring과 코루틴 통합
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
}

tasks.test {
    enabled = false
}
plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

springBoot {
    mainClass.set("com.project.api.ApiApplicationKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation("org.postgresql:postgresql")

    // Kotlin 코루틴 기본
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Spring과 코루틴 통합
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // WebFlux 기반 비동기 처리 시 필요



}

tasks.test {
    enabled = false
}
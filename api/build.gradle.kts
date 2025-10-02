plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

springBoot {
    mainClass.set("io.cloudtype.api.ApiApplicationKt")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("com.h2database:h2")
}

tasks.test {
    enabled = false
}
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("kapt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
//    implementation("org.jetbrains.kotlin:kotlin-reflect")
//    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
//    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    kapt("org.springframework.boot:spring-boot-configuration-processor") // Kotlin Annotation Processing을 쓰는 경우
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
}


plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"

    id("org.jetbrains.kotlin.jvm") version "2.3.21"
    id("org.jetbrains.kotlin.plugin.spring") version "2.3.21"
}

group = "in.hridaykh"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
}

dependencies {
    // Standard Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Development & Runtime
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Supabase Dependencies
    implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")

    // Add Ktor's engine (required underneath by Supabase-kt client)
    implementation("io.ktor:ktor-client-okhttp:3.0.0")

    // Kotlin Standard Library (Required to mix Kotlin code)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")

}

tasks.withType<Test> {
    useJUnitPlatform()
}
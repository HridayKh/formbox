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
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.postgresql:postgresql")

	// caching
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	// lombok
	compileOnly("org.projectlombok:lombok:1.18.46")
	annotationProcessor("org.projectlombok:lombok:1.18.46")

	// Supabase Dependencies
	implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
	implementation("io.github.jan-tennert.supabase:auth-kt")
	implementation("io.ktor:ktor-client-okhttp:3.0.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")

	// Resend and Polar
	implementation("com.resend:resend-java:+")
	implementation("com.jsandev.polar:polar-java-sdk:0.1.5")
	implementation("com.jsandev.polar:polar-spring:0.1.5")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
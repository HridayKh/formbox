buildscript {
	repositories {
		mavenCentral()
	}
}

plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"

	id("org.jetbrains.kotlin.jvm") version "2.3.21"
	id("org.jetbrains.kotlin.plugin.spring") version "2.3.21"

	id("io.sentry.jvm.gradle") version "6.14.0"
}

repositories {
	mavenCentral()
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

kotlin {
	jvmToolchain(25)
}

sentry {
	includeSourceContext = true

	org = "hriday-jg"
	projectName = "formbox"
	authToken = System.getenv("SENTRY_AUTH_TOKEN")
}
extra["opentelemetry.version"] = "1.63.0"
dependencies {
	// Standard Spring Boot dependencies
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.postgresql:postgresql")

	// logging

	// logging
//	implementation(platform("io.sentry:sentry-bom:8.48.0"))
	implementation("io.sentry:sentry-spring-boot-4:8.48.0")
	implementation("io.sentry:sentry-async-profiler:8.48.0")
	implementation("io.sentry:sentry-logback:8.48.0")
	implementation("io.sentry:sentry-opentelemetry-agentless-spring:8.48.0")
	implementation(platform("io.opentelemetry:opentelemetry-bom:1.63.0"))
	implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.29.0"))
	implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")

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
	implementation("com.jsandev.polar:polar-java-sdk:0.1.5")
	implementation("com.jsandev.polar:polar-spring:0.1.5")
}

group = "in.hridaykh"
version = "0.0.1-SNAPSHOT"

tasks.withType<Test> {
	useJUnitPlatform()
}
/**
 * File Path: yakki-core/build.gradle.kts
 * File Name: build.gradle.kts
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Build configuration for Yakki Core - Pure Kotlin JVM module.
 * Contains all business logic, models, and utilities that can be shared across platforms.
 *
 * No Android dependencies - pure Kotlin for maximum portability.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

// Use standard Kotlin source layout
sourceSets {
    main {
        kotlin.srcDirs("src/commonMain/kotlin")
    }
    test {
        kotlin.srcDirs("src/commonTest/kotlin")
    }
}

dependencies {
    // Pure Kotlin dependencies only - NO Android!

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

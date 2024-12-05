plugins {
    kotlin("jvm") version "1.9.23" // Kotlin plugin version
    application // Apply the application plugin to run the application
}

repositories {
    mavenCentral() // Maven central repository to fetch dependencies from
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Coroutines for concurrency
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.10.0") // MongoDB driver for Kotlin coroutines
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Set Java 21 as the JDK version
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21" // Ensure Kotlin compiles to Java 21 bytecode
    }
}

application {
    mainClass.set("Database_testKt") // Set the main class to the Kotlin file's name
}

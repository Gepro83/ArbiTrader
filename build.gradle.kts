plugins {
    kotlin("jvm") version "1.3.61"
}

group = "at.gpro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("org.slf4j:slf4j-jdk14:1.7.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
}

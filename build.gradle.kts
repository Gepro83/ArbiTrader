
plugins {
    kotlin("jvm") version "1.4.10"
    idea
}

group = "at.gpro"
version = "1.0-SNAPSHOT"

idea.module {
    isDownloadJavadoc = true
    isDownloadSources = true
}

repositories {
    mavenCentral()
}

dependencies {
    // general
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
    implementation("com.google.code.gson:gson:2.8.6")
    
    // xchange
    implementation("org.knowm.xchange:xchange-core:5.0.4")
    implementation("org.knowm.xchange:xchange-stream-coinbasepro:5.0.4")
    implementation("org.knowm.xchange:xchange-stream-kraken:5.0.4")
    implementation("org.knowm.xchange:xchange-stream-bitstamp:5.0.4")
    implementation("org.knowm.xchange:xchange-stream-binance:5.0.4")
    
    
    // test
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation("io.mockk:mockk:1.10.2")
}

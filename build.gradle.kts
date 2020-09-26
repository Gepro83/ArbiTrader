
plugins {
    kotlin("jvm") version "1.3.61"
}

group = "at.gpro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // general
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
    
    // xchange
    implementation("org.knowm.xchange:xchange-core:5.0.2")
    implementation("org.knowm.xchange:xchange-stream-coinbasepro:5.0.2")
    implementation("org.knowm.xchange:xchange-stream-kraken:5.0.2")
    implementation("org.knowm.xchange:xchange-stream-bitstamp:5.0.2")
    implementation("org.knowm.xchange:xchange-stream-binance:5.0.2")
    
    
    // test
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
}

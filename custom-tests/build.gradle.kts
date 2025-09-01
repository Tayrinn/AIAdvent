plugins {
    kotlin("jvm") version "1.9.10"
    id("java")
}

// Using system JVM

// Using system JVM

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation(project(":shared"))
}

tasks.test {
    testLogging {
        showStandardStreams = true
    }
}

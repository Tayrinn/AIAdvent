plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
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

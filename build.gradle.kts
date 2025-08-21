// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10" apply false
    id("org.jetbrains.compose") version "1.5.11" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

subprojects {
    group = "com.tayrinn.aiadvent"
    version = "1.0.0"
}

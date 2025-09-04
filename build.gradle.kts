// ⚠️ ВАЖНО: НЕ УДАЛЯТЬ ЭТОТ ФАЙЛ!
// Этот файл критически важен для работы Gradle проекта
// Без него проект не соберется!

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

// Repositories are now managed by dependencyResolutionManagement in settings.gradle.kts

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

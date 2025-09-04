// ⚠️ ВАЖНО: НЕ УДАЛЯТЬ ЭТОТ ФАЙЛ!
// Этот файл критически важен для работы Gradle проекта
// Без него проект не соберется!

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "AIAdvent"

include(":shared")
include(":desktop")
include(":app")
// include(":custom-tests") // Temporarily disabled due to Kotlin version conflict

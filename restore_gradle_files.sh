#!/bin/bash

# Скрипт для восстановления критически важных Gradle файлов
echo "🔧 Проверяю и восстанавливаю Gradle файлы..."

# Проверяем settings.gradle.kts
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ settings.gradle.kts не найден! Восстанавливаю..."
    cat > settings.gradle.kts << 'EOF'
// ⚠️ ВАЖНО: НЕ УДАЛЯТЬ ЭТОТ ФАЙЛ!
// Этот файл критически важен для работы Gradle проекта
// Без него проект не соберется!

rootProject.name = "AIAdvent"

include(":shared")
include(":desktop")
EOF
    echo "✅ settings.gradle.kts восстановлен"
fi

# Проверяем build.gradle.kts
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ build.gradle.kts не найден! Восстанавливаю..."
    cat > build.gradle.kts << 'EOF'
// ⚠️ ВАЖНО: НЕ УДАЛЯТЬ ЭТОТ ФАЙЛ!
// Этот файл критически важен для работы Gradle проекта
// Без него проект не соберется!

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

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
EOF
    echo "✅ build.gradle.kts восстановлен"
fi

echo "🔍 Проверяю права доступа..."
chmod 644 *.gradle.kts 2>/dev/null

echo "✅ Проверка завершена"
echo "📁 Текущие файлы:"
ls -la *.gradle.kts 2>/dev/null || echo "❌ Gradle файлы не найдены!"



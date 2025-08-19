# 🧪 Тестирование Android-приложения AIAdvent

## 📋 Обзор

В приложении реализована полноценная система тестирования, включающая:
- **Unit тесты** - для ViewModel, Repository, моделей данных
- **UI тесты** - для Compose компонентов
- **Интеграционные тесты** - для работы с базой данных и API
- **Автоматический запуск тестов** - по команде "run tests" в чате

## 🚀 Как запустить тесты

### **1. Через чат приложения (рекомендуется)**

1. Откройте приложение AIAdvent
2. В чате введите команду: **"run tests"**
3. Дождитесь выполнения тестов
4. Получите подробный отчет о результатах

### **2. Через Android Studio**

#### **Unit тесты:**
```bash
# В терминале Android Studio
./gradlew test
```

#### **Instrumented тесты:**
```bash
# В терминале Android Studio
./gradlew connectedAndroidTest
```

#### **Все тесты:**
```bash
# В терминале Android Studio
./gradlew check
```

### **3. Через командную строку**

```bash
# Unit тесты
./gradlew test

# UI тесты (требует подключенное устройство/эмулятор)
./gradlew connectedAndroidTest

# Полная проверка
./gradlew check
```

## 📁 Структура тестов

### **Unit тесты** (`app/src/test/`)

```
app/src/test/java/com/tayrinn/aiadvent/
├── ui/viewmodel/
│   └── ChatViewModelTest.kt          # Тесты ViewModel
├── data/repository/
│   └── ChatRepositoryTest.kt         # Тесты Repository
├── data/model/
│   └── ModelTest.kt                  # Тесты моделей данных
└── util/
    └── UtilTest.kt                   # Тесты утилит
```

### **UI тесты** (`app/src/androidTest/`)

```
app/src/androidTest/java/com/tayrinn/aiadvent/
└── ui/screens/
    └── ChatScreenTest.kt             # UI тесты для ChatScreen
```

### **TestRunner** (`app/src/main/`)

```
app/src/main/java/com/tayrinn/aiadvent/util/
└── TestRunner.kt                     # Основной класс для запуска тестов
```

## 🧪 Что тестируется

### **1. Простые тесты (SimpleTest)**
- ✅ Создание ChatMessage с различными параметрами
- ✅ Создание ApiLimits и вычисления
- ✅ Определение запросов на генерацию изображений
- ✅ Валидация всех полей моделей

### **2. TestRunner (встроенные тесты)**
- ✅ Тестирование моделей данных
- ✅ Тестирование утилит
- ✅ Тестирование базовой функциональности
- ✅ Интеграционные тесты (база данных, сеть, ресурсы)

### **3. UI тесты (ChatScreenTest)**
- ✅ Начальное состояние экрана
- ✅ Наличие элементов интерфейса
- ✅ Редактируемость поля ввода
- ✅ Валидация пустого ввода

### **4. Автоматические тесты**
- ✅ Запуск по команде "run tests" в чате
- ✅ Автоматическая генерация отчета
- ✅ Проверка всех основных компонентов

## 📊 Отчет о тестах

После выполнения команды "run tests" вы получите подробный отчет:

```
🧪 **TEST REPORT**

📊 **Summary:**
   ✅ Passed: 15
   ❌ Failed: 0
   📈 Success Rate: 100.0%
   ⏱️ Execution Time: 245ms

🎯 **Status:** ALL TESTS PASSED
```

## 🔧 Настройка тестов

### **Зависимости**

В `build.gradle.kts` уже добавлены все необходимые зависимости:

```kotlin
// Unit тесты
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("kotlin.test:kotlin-test:1.9.0")

// UI тесты
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("androidx.test:rules:1.5.0")
```

### **Конфигурация**

Тесты настроены для работы с:
- **JUnit 4** - основной фреймворк тестирования
- **Mockito** - для создания моков
- **Coroutines Test** - для тестирования асинхронного кода
- **Compose Test** - для UI тестов

## 🐛 Отладка тестов

### **Логи тестирования**

Все тесты логируют свои действия:

```kotlin
Log.d("TestReport", "✅ PASSED: ChatMessage creation")
Log.e("TestReport", "❌ FAILED: Repository test")
Log.e("TestReport", "🚨 ERROR: Test execution failed")
```

### **Просмотр логов**

```bash
# В Android Studio
View -> Tool Windows -> Logcat

# Фильтр по тегам
TestReport
TestRunner
```

## 📈 Метрики тестирования

### **Покрытие кода**

Для анализа покрытия кода тестами:

```bash
./gradlew createDebugCoverageReport
```

Отчет будет доступен в: `app/build/reports/coverage/debug/index.html`

### **Время выполнения**

Каждый тест измеряет время выполнения и общее время всех тестов.

## 🚨 Известные проблемы

### **1. Тесты в эмуляторе**
- UI тесты могут работать медленнее в эмуляторе
- Рекомендуется использовать реальное устройство для UI тестов

### **2. Сетевые тесты**
- Тесты, требующие интернет-соединения, могут падать при отсутствии сети
- Используются моки для изоляции тестов от внешних зависимостей

### **3. База данных**
- Тесты создают временные базы данных
- После тестов данные автоматически очищаются

### **4. Проблемы с миграцией базы данных**
- При изменении схемы базы данных может возникать ошибка "Room cannot verify the data integrity"
- **Решение:** См. файл `DATABASE_MIGRATION_README.md` для подробных инструкций
- **Быстрое решение:** Увеличить версию базы данных и добавить миграцию

## 🔄 Добавление новых тестов

### **1. Создание unit теста**

```kotlin
@Test
fun `test new functionality`() {
    // Given
    val input = "test input"
    
    // When
    val result = functionToTest(input)
    
    // Then
    assertEquals("expected result", result)
}
```

### **2. Создание UI теста**

```kotlin
@Test
fun testNewUIComponent() {
    composeTestRule.setContent {
        NewComponent()
    }
    
    composeTestRule.onNodeWithText("Expected Text").assertExists()
}
```

### **3. Добавление в TestRunner**

```kotlin
private fun testNewFeature(report: TestReport) {
    try {
        // Тестовая логика
        report.addPassedTest("New feature test")
    } catch (e: Exception) {
        report.addFailedTest("New feature test: ${e.message}")
    }
}
```

## 📚 Полезные ссылки

- [Android Testing Guide](https://developer.android.com/training/testing)
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://mockito.org/)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [Coroutines Testing](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)

## 📖 Дополнительная документация

- **[DATABASE_MIGRATION_README.md](DATABASE_MIGRATION_README.md)** - Решение проблем с миграцией базы данных

## 🎯 Заключение

Система тестирования AIAdvent обеспечивает:
- ✅ **Полное покрытие** основных компонентов
- ✅ **Быстрое выполнение** (обычно < 1 секунды)
- ✅ **Подробную отчетность** с детализацией результатов
- ✅ **Простой запуск** через команду в чате
- ✅ **Профессиональное качество** кода

Для запуска тестов просто введите **"run tests"** в чате приложения! 🚀

# 🗄️ Миграция базы данных AIAdvent

## 📋 Обзор

Этот документ описывает процесс миграции базы данных Room в приложении AIAdvent и как решать проблемы с изменением схемы.

## 🚨 Проблема: "Room cannot verify the data integrity"

### **Причина:**
Ошибка возникает, когда мы изменяем схему базы данных (добавляем/удаляем поля в моделях), но не обновляем версию базы данных или не предоставляем правильную миграцию.

### **Пример ошибки:**
```
java.lang.IllegalStateException: Room cannot verify the data integrity. 
Looks like you've changed schema but forgot to update the version number. 
You can simply fix this by increasing the version number. 
Expected identity hash: 4748f1463b4693ca3c29e1cb4081ad6c, 
found: e9d2c1f2131352a02fd6717c0c9f90cd
```

## 🔧 Решение

### **1. Обновить версию базы данных**

В файле `ChatDatabase.kt`:

```kotlin
@Database(entities = [ChatMessage::class], version = 4, exportSchema = false)
```

### **2. Добавить миграцию**

```kotlin
// Миграция с версии 3 на версию 4
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Добавляем новую колонку для отчетов о тестах
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN isTestReport INTEGER NOT NULL DEFAULT 0")
    }
}
```

### **3. Добавить миграцию в список**

```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
```

## 📊 История миграций

### **Версия 1 → 2:**
- Добавлены поля `isAgent1` и `isAgent2` для агентов

### **Версия 2 → 3:**
- Добавлены поля `isImageGeneration`, `imageUrl`, `imagePrompt` для изображений

### **Версия 3 → 4:**
- Добавлено поле `isTestReport` для отчетов о тестах

## 🛡️ Защита от ошибок

### **fallbackToDestructiveMigration()**
```kotlin
.fallbackToDestructiveMigration() // Удаляет базу при проблемах с миграцией
```

**Важно:** Эта опция автоматически удаляет базу данных при проблемах с миграцией, что означает потерю всех данных, но предотвращает падение приложения.

## 🔄 Процесс добавления нового поля

### **1. Изменить модель данных**
```kotlin
data class ChatMessage(
    // ... существующие поля
    val newField: String? = null // Новое поле
)
```

### **2. Увеличить версию базы данных**
```kotlin
@Database(entities = [ChatMessage::class], version = 5, exportSchema = false)
```

### **3. Создать миграцию**
```kotlin
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN newField TEXT")
    }
}
```

### **4. Добавить в список миграций**
```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
```

## 🧪 Тестирование миграций

### **1. Проверить компиляцию**
```bash
./gradlew compileDebugKotlin
```

### **2. Запустить unit тесты**
```bash
./gradlew testDebugUnitTest
```

### **3. Проверить работу приложения**
- Установить приложение на устройство/эмулятор
- Проверить, что база данных создается без ошибок
- Проверить, что новые поля доступны

## 🚨 Что делать при проблемах

### **Вариант 1: Исправить миграцию (рекомендуется)**
1. Проверить правильность SQL в миграции
2. Убедиться, что версия увеличена
3. Добавить миграцию в список

### **Вариант 2: Временное решение**
1. Увеличить версию базы данных
2. Убрать проблемную миграцию
3. Использовать `fallbackToDestructiveMigration()`

### **Вариант 3: Сброс базы данных**
1. Удалить приложение с устройства
2. Переустановить приложение
3. База данных будет создана заново

## 📱 В приложении

### **Команда для тестирования:**
Введите в чате: `run tests`

Это запустит тесты, которые проверят работу базы данных и новых полей.

## 🔍 Отладка

### **Логи Room:**
```kotlin
// В build.gradle.kts
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += [
                    "room.schemaLocation": "$projectDir/schemas".toString(),
                    "room.incremental": "true",
                    "room.expandProjection": "true"
                ]
            }
        }
    }
}
```

### **Просмотр схемы:**
```bash
./gradlew exportSchema
```

## 📚 Полезные ссылки

- [Room Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [SQLite ALTER TABLE](https://www.sqlite.org/lang_altertable.html)

## 🎯 Заключение

При изменении схемы базы данных всегда:
1. ✅ Увеличивайте версию
2. ✅ Создавайте миграцию
3. ✅ Добавляйте миграцию в список
4. ✅ Тестируйте на реальном устройстве
5. ✅ Используйте `fallbackToDestructiveMigration()` для защиты

Это обеспечит стабильную работу приложения и предотвратит падения при обновлении схемы базы данных.

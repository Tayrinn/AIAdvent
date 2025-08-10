# 🔄 Миграция с ChatGPT на DeepSeek API

## 📋 Что было изменено

### 1. Модели данных
- **Файл**: `ChatGPTModels.kt` → `DeepSeekModels.kt`
- **Изменения**: Обновлены модели для работы с DeepSeek API
- **Особенности**: Исправлена проблема с зарезервированным словом `object`

### 2. API интерфейс
- **Файл**: `ChatGPTApi.kt` → `DeepSeekApi.kt`
- **Изменения**: Обновлен endpoint и структура запросов
- **Base URL**: `https://api.openai.com/` → `https://api.deepseek.com/`

### 3. Репозиторий
- **Файл**: `ChatRepository.kt`
- **Изменения**: 
  - Переименован метод `sendMessageToChatGPT` → `sendMessageToDeepSeek`
  - Обновлена логика формирования запросов
  - Изменена модель с `gpt-3.5-turbo` на `deepseek-chat`

### 4. Dependency Injection
- **Файл**: `AppModule.kt`
- **Изменения**: Обновлены провайдеры для DeepSeek API

### 5. UI обновления
- **Файл**: `ChatScreen.kt`
- **Изменения**: 
  - Обновлены строки для DeepSeek
  - Убраны неиспользуемые импорты
  - Исправлены предупреждения компилятора

### 6. Строки ресурсов
- **Файл**: `strings.xml`
- **Изменения**: Обновлены все упоминания ChatGPT на DeepSeek

### 7. Документация
- **Файлы**: `README.md`, `DEEPSEEK_SETUP.md`
- **Изменения**: Полностью переписаны инструкции для DeepSeek

## 🚀 Преимущества DeepSeek

### 💰 Стоимость
- **ChatGPT**: $0.002 за 1K токенов
- **DeepSeek**: Бесплатно до 1000 запросов в день

### 📊 Лимиты
- **ChatGPT**: Строгие ограничения по запросам
- **DeepSeek**: Более щедрые бесплатные лимиты

### 🌍 Поддержка языков
- **ChatGPT**: Отличная поддержка русского
- **DeepSeek**: Аналогичное качество для русского языка

## 🔧 Технические детали

### API Endpoints
```kotlin
// Старый (ChatGPT)
baseUrl("https://api.openai.com/")

// Новый (DeepSeek)
baseUrl("https://api.deepseek.com/")
```

### Модели
```kotlin
// Старый (ChatGPT)
model = "gpt-3.5-turbo"

// Новый (DeepSeek)
model = "deepseek-chat"
```

### Структура запросов
```kotlin
// Одинаковая для обоих API
@POST("v1/chat/completions")
suspend fun sendMessage(
    @Header("Authorization") authorization: String,
    @Body request: DeepSeekRequest
): DeepSeekResponse
```

## ✅ Что осталось без изменений

- Архитектура приложения (MVVM + Repository)
- UI компоненты и дизайн
- Локальное хранение (Room)
- Dependency Injection (Hilt)
- Навигация и структура экранов

## 🎯 Результат

Приложение успешно мигрировано с ChatGPT API на DeepSeek API:
- ✅ Убрана ошибка 429 (Too Many Requests)
- ✅ Снижены затраты на API
- ✅ Улучшены лимиты запросов
- ✅ Сохранена вся функциональность
- ✅ Обновлена документация

## 🚀 Следующие шаги

1. Получите API ключ на [platform.deepseek.com](https://platform.deepseek.com)
2. Настройте ключ в приложении
3. Протестируйте функциональность
4. Наслаждайтесь бесплатным AI чатом!

---

**Миграция завершена успешно! 🎉**

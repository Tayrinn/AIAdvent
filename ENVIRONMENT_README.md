# 🔐 Настройка переменных окружения для AIAdvent Desktop

Это руководство поможет настроить переменные окружения для безопасного хранения API ключей.

## 📁 Файлы конфигурации

### `env.local` - Основной файл конфигурации
- **Содержит:** Реальные API ключи и настройки
- **Статус:** Защищен от коммита в Git
- **Использование:** Только для локальной разработки

### `env.local.example` - Пример конфигурации
- **Содержит:** Шаблон с placeholder'ами
- **Статус:** Добавлен в Git для документации
- **Использование:** Для других разработчиков

## 🚀 Быстрая настройка

### 1. Создание конфигурации
```bash
# Скопируйте пример
cp env.local.example env.local

# Отредактируйте API ключи
nano env.local
```

### 2. Заполнение API ключей
```bash
# JetBrains Marketplace API Key
JETBRAINS_API_KEY=your_actual_jetbrains_api_key_here

# OpenAI API Key
OPENAI_API_KEY=your_actual_openai_api_key_here
```

## 🔑 Обязательные переменные

### JetBrains Marketplace
```bash
JETBRAINS_API_KEY=perm-XXXXX...
```
- **Назначение:** Публикация плагина в JetBrains Marketplace
- **Получение:** [plugins.jetbrains.com](https://plugins.jetbrains.com) → API Keys

### OpenAI API
```bash
OPENAI_API_KEY=sk-proj-XXXXX...
```
- **Назначение:** Интеграция с ChatGPT для AI-функций
- **Получение:** [platform.openai.com](https://platform.openai.com) → API Keys

## 🎯 Дополнительные настройки

### OpenAI параметры
```bash
OPENAI_MODEL=gpt-3.5-turbo
OPENAI_MAX_TOKENS=2000
OPENAI_TEMPERATURE=0.7
```

### Плагин
```bash
PLUGIN_NAME=AIAdvent Desktop
PLUGIN_ID=com.tayrinn.aiadvent.desktop
PLUGIN_VERSION=1.0.0
```

## 🚀 Запуск приложения

### Способ 1: Автоматическая загрузка
```bash
./load_env.sh
```
- ✅ Автоматически загружает переменные
- ✅ Проверяет наличие ключей
- ✅ Запускает приложение

### Способ 2: Ручная загрузка
```bash
# Загружаем переменные
source env.local

# Запускаем приложение
./gradlew :desktop:run
```

### Способ 3: Прямой запуск
```bash
# Переменные загружаются автоматически из System.getenv()
./gradlew :desktop:run
```

## 🔒 Безопасность

### ✅ Что защищено
- `env.local` - не коммитится в Git
- API ключи не видны в коде
- Разные ключи для разных окружений

### ❌ Что НЕ защищено
- `env.local.example` - содержит placeholder'ы
- Локальные файлы на вашем компьютере
- Переменные окружения в памяти

## 🚨 Важные моменты

### 1. Никогда не коммитьте `env.local`
```bash
# Проверьте .gitignore
cat .gitignore | grep env.local
```

### 2. Регулярно обновляйте API ключи
```bash
# Проверьте актуальность ключей
curl -H "Authorization: Bearer $OPENAI_API_KEY" \
     "https://api.openai.com/v1/models"
```

### 3. Используйте разные ключи для разных окружений
```bash
# Разработка
env.local.dev

# Продакшн
env.local.prod

# Тестирование
env.local.test
```

## 🔧 Устранение проблем

### Ошибка: "API key not found"
```bash
# Проверьте наличие файла
ls -la env.local

# Проверьте содержимое
cat env.local | grep OPENAI_API_KEY
```

### Ошибка: "Unauthorized"
```bash
# Проверьте правильность ключа
echo $OPENAI_API_KEY

# Проверьте права доступа
curl -H "Authorization: Bearer $OPENAI_API_KEY" \
     "https://api.openai.com/v1/models"
```

## 📚 Дополнительные ресурсы

- [JetBrains Marketplace API](https://plugins.jetbrains.com/docs/marketplace/marketplace-rest-api.html)
- [OpenAI API Documentation](https://platform.openai.com/docs/api-reference)
- [Environment Variables Best Practices](https://12factor.net/config)

---

*Создано для проекта AIAdvent Desktop*  
*Автор: Tayrinn*  
*Версия: 1.0.0*

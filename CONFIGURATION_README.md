# 🔐 Конфигурация AIAdvent Desktop

## 📋 Обзор

Этот проект использует конфигурационные файлы для хранения чувствительных данных, таких как API ключи. Все конфигурационные файлы исключены из Git для безопасности.

## ⚙️ Настройка

### 1. Создание файла конфигурации

Скопируйте пример конфигурации:

```bash
cp config.properties.example config.properties
```

### 2. Заполнение API ключей

Откройте `config.properties` и замените placeholder на ваш реальный API ключ:

```properties
# OpenAI API Configuration
openai.api.key=YOUR_ACTUAL_OPENAI_API_KEY_HERE
openai.api.model=gpt-3.5-turbo
openai.api.max_tokens=2000
openai.api.temperature=0.7
```

### 3. Получение OpenAI API ключа

1. Перейдите на [OpenAI Platform](https://platform.openai.com/)
2. Войдите в свой аккаунт
3. Перейдите в раздел "API Keys"
4. Создайте новый API ключ
5. Скопируйте ключ в `config.properties`

## 🚫 Безопасность

### Файлы, исключенные из Git:

- `config.properties` - содержит реальные API ключи
- `*.env` - файлы с переменными окружения
- `local.properties` - локальные настройки

### Файлы, включенные в Git:

- `config.properties.example` - пример конфигурации без ключей
- `.gitignore` - правила исключения файлов

## 🔧 Структура конфигурации

```properties
# OpenAI API
openai.api.key=sk-proj-...          # Ваш API ключ
openai.api.model=gpt-3.5-turbo      # Модель для использования
openai.api.max_tokens=2000          # Максимальное количество токенов
openai.api.temperature=0.7          # Температура генерации (0.0-1.0)
```

## 🚨 Важные замечания

1. **Никогда не коммитьте `config.properties` в Git**
2. **Храните API ключи в безопасном месте**
3. **Не передавайте конфигурационные файлы третьим лицам**
4. **Регулярно обновляйте API ключи**

## 🆘 Устранение неполадок

### Ошибка "API key not found"

Проверьте, что файл `config.properties` существует и содержит правильный API ключ.

### Ошибка "Invalid API key"

Убедитесь, что API ключ скопирован полностью, без лишних пробелов.

### Ошибка "Configuration file not found"

Убедитесь, что файл `config.properties` находится в корневой папке проекта.

## 📚 Дополнительные ресурсы

- [OpenAI API Documentation](https://platform.openai.com/docs/api-reference)
- [OpenAI Pricing](https://openai.com/pricing)
- [Security Best Practices](https://platform.openai.com/docs/guides/security)

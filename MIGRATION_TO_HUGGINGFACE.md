# 🔄 Миграция на Hugging Face Inference API

## ✅ Что было изменено

### 1. **Модели данных** (`HuggingFaceModels.kt`)
- Создан новый файл с моделями для Hugging Face
- `HuggingFaceRequest` - запрос к API
- `HuggingFaceResponse` - ответ от API
- `HuggingFaceParameters` - параметры генерации

### 2. **API интерфейс** (`HuggingFaceApi.kt`)
- Заменен `DeepSeekApi` на `HuggingFaceApi`
- **Основная модель**: `gpt2` - стабильная и быстрая
- **Альтернативная модель**: `microsoft/DialoGPT-small` - для диалогов
- **Резервная модель**: `distilgpt2` - всегда доступна
- Base URL: `https://api-inference.huggingface.co/`

### 3. **Репозиторий** (`ChatRepository.kt`)
- Метод `sendMessageToDeepSeek` → `sendMessageToHuggingFace`
- Изменена логика формирования запроса под Hugging Face API
- Контекст формируется как текст с ролями "User:" и "Assistant:"
- **Автоматическое переключение между моделями** при ошибках
- Улучшена обработка ошибок и fallback логика

### 4. **Dependency Injection** (`AppModule.kt`)
- `provideDeepSeekApi` → `provideHuggingFaceApi`
- Обновлен base URL и тип API

### 5. **ViewModel** (`ChatViewModel.kt`)
- Обновлен вызов метода репозитория
- Изменены комментарии

### 6. **Строки ресурсов** (`strings.xml`)
- Обновлены тексты для Hugging Face
- Изменен placeholder с `sk-...` на `hf_...`
- Обновлена ссылка на получение API ключа

### 7. **Документация**
- Создан `HUGGINGFACE_SETUP.md` с подробными инструкциями
- Обновлен `README.md`
- Удалены старые файлы DeepSeek

## 🗑️ Удаленные файлы
- `DeepSeekModels.kt`
- `DeepSeekApi.kt`

## 🆕 Новые файлы
- `HuggingFaceModels.kt`
- `HuggingFaceApi.kt`
- `HUGGINGFACE_SETUP.md`

## 🎯 Преимущества Hugging Face

✅ **30,000 бесплатных запросов в месяц**  
✅ **Тысячи открытых моделей**  
✅ **Быстрая интеграция**  
✅ **Активное сообщество**  
✅ **Регулярные обновления**  
✅ **Автоматическое переключение между моделями**  

## 🛡️ Надежность

Приложение теперь использует **систему fallback**:
1. **Пробует основную модель** (`gpt2`)
2. **При ошибке переключается** на альтернативную (`DialoGPT-small`)
3. **В крайнем случае использует** резервную (`distilgpt2`)

Это обеспечивает **максимальную доступность** и **стабильную работу**!

## 🚀 Готово к использованию!

Приложение успешно мигрировано на Hugging Face Inference API и готово к тестированию!

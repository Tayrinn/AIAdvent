# AI Advent - Android Chat App with AI Image Generation

Android приложение для чата с ИИ и генерации изображений через Kandinsky API.

## 🚀 Возможности

- 💬 Чат с двумя ИИ агентами (Ollama)
- 🎨 Генерация изображений через Kandinsky API
- 📱 Современный UI на Jetpack Compose
- 🔄 Автоматическое обновление сообщений
- 🖼️ Отображение сгенерированных изображений

## 🛠️ Технологии

- **Android**: Kotlin, Jetpack Compose, Material Design 3
- **Backend**: Ollama (локальный ИИ), FusionBrain API (Kandinsky)
- **Архитектура**: MVVM, Repository Pattern, Dependency Injection (Hilt)
- **База данных**: Room Database
- **Сеть**: Retrofit, OkHttp
- **Изображения**: Coil

## 📋 Требования

- Android Studio Hedgehog или новее
- Android SDK 34+
- Python 3.8+ (для MCP сервера)
- Ollama (локально установленный)

## ⚙️ Установка

### 1. Клонирование репозитория

```bash
git clone https://github.com/yourusername/aiadvent.git
cd aiadvent
```

### 2. Настройка API ключей

Создайте файл `.env` на основе `.env.example`:

```bash
cp .env.example .env
```

Отредактируйте `.env` и добавьте ваши API ключи:

```env
# Kandinsky API Keys
# Получите ключи на https://www.segmind.com/
KANDINSKY_API_KEY=your_api_key_here
KANDINSKY_SECRET_KEY=your_secret_key_here

# MCP Server Configuration
MCP_SERVER_HOST=0.0.0.0
MCP_SERVER_PORT=8000
```

### 3. Установка Python зависимостей

```bash
cd mcp_env
pip install -r requirements.txt
```

### 4. Запуск MCP сервера

```bash
cd ..
source mcp_env/bin/activate
python mcp_server.py
```

### 5. Сборка Android приложения

Откройте проект в Android Studio и соберите APK:

```bash
./gradlew assembleDebug
```

## 🔧 Настройка Ollama

1. Установите Ollama: https://ollama.ai/
2. Запустите Ollama сервер
3. Скачайте модели:

```bash
ollama pull phi3
ollama pull llama2
```

## 📱 Использование

1. Запустите MCP сервер
2. Установите APK на устройство/эмулятор
3. Настройте IP адрес Ollama сервера в настройках
4. Начните чат!

### Генерация изображений

Для генерации изображения используйте команды:
- "сгенерируй изображение кота"
- "generate cat image"
- "нарисуй собаку"

## 🏗️ Архитектура

```
app/
├── data/
│   ├── api/          # Retrofit API интерфейсы
│   ├── database/     # Room база данных
│   ├── model/        # Data классы
│   ├── repository/   # Repository слой
│   └── service/      # Сервисы (генерация изображений)
├── di/               # Hilt dependency injection
├── ui/
│   ├── screens/      # Compose экраны
│   ├── theme/        # UI темы
│   └── viewmodel/    # ViewModels
└── util/             # Утилиты
```

## 🔒 Безопасность

- API ключи хранятся в `.env` файле (не коммитится в git)
- `.env.example` содержит шаблон без реальных ключей
- Все секретные данные исключены из репозитория

## 📝 Лицензия

MIT License

## 🤝 Вклад в проект

1. Fork репозитория
2. Создайте feature branch
3. Commit изменения
4. Push в branch
5. Создайте Pull Request

## 🐛 Проблемы

Если у вас возникли проблемы:

1. Проверьте логи MCP сервера
2. Убедитесь, что API ключи корректны
3. Проверьте подключение к Ollama серверу
4. Создайте Issue с описанием проблемы

## 📞 Поддержка

- Создайте Issue в GitHub
- Опишите проблему подробно
- Приложите логи и скриншоты

# 🚀 Быстрый старт - AI Code Analysis & Image Generation Server

## 🔥 3 шага до публичного доступа (без доступа к роутеру)

### Шаг 1: Установка Ngrok (1 минута)
```bash
./setup_ngrok.sh
```

### Шаг 2: Запуск сервера и туннеля
```bash
# Терминал 1 - запуск сервера
./start_server_with_tunnel.sh --server

# Терминал 2 - запуск туннеля
./start_server_with_tunnel.sh --tunnel
```

### Шаг 3: Получение публичного URL
После запуска вы увидите:
```
Forwarding    https://abc123.ngrok.io -> http://localhost:8080
```

## 🎯 Что доступно по публичному URL

### Веб-интерфейс генерации изображений
```
https://abc123.ngrok.io/generate
```
- 🎨 Форма для ввода промптов
- 🎭 Выбор стиля (DEFAULT, KANDINSKY, UHD, ANIME)
- 📐 Настройка размера (256x256 до 2048x2048)
- 🚫 Негативные промпты
- 💡 Примеры промптов

### API для генерации изображений
```bash
curl -X POST https://abc123.ngrok.io/api/generate-image \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "красивый закат над горами",
    "style": "DEFAULT",
    "width": 1024,
    "height": 1024
  }'
```

### Анализ кода
```bash
curl -X POST https://abc123.ngrok.io/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "fun test() { println(\"Hello\") }",
    "fileName": "Test.kt"
  }'
```

## 🛠️ Доступные скрипты

| Скрипт | Назначение |
|--------|------------|
| `./setup_ngrok.sh` | Установка ngrok |
| `./start_server_with_tunnel.sh` | Запуск сервера с туннелем |
| `./generate_image_demo.sh` | Демо генерации изображений |
| `./test_server.sh` | Тестирование всех функций |

## 🌐 Альтернативные варианты (если ngrok не подходит)

### LocalTunnel (бесплатно)
```bash
npm install -g localtunnel
npx localtunnel --port 8080
```

### Cloudflare Tunnel
```bash
# Установка
brew install cloudflare/cloudflare/cloudflared

# Создание туннеля
cloudflared tunnel create my-ai-server
cloudflared tunnel run my-ai-server --url http://localhost:8080
```

### Serveo (через SSH)
```bash
ssh -R 80:localhost:8080 serveo.net
```

## 💰 Для постоянного доступа - VPS

Если нужен постоянный публичный доступ:

1. **DigitalOcean** - от $6/месяц
2. **Hetzner** - от €3/месяц
3. **Vultr** - от $2.5/месяц

### Быстрая настройка VPS:
```bash
# 1. Установить Java
sudo apt update && sudo apt install openjdk-17-jdk

# 2. Скопировать проект
git clone your-repo
cd AIAdvent

# 3. Запустить сервер
./gradlew :shared:runWebServer
```

## 📊 Мониторинг и отладка

### Логи сервера
```bash
# Все логи в реальном времени
tail -f server.log
```

### Проверка здоровья
```bash
curl https://your-ngrok-url.ngrok.io/api/health
```

### Тестирование API
```bash
./test_server.sh
```

## 🚨 Устранение проблем

### Сервер не запускается
```bash
# Проверить порт 8080
lsof -i :8080

# Проверить Java
java -version

# Проверить API ключи
echo $OPENAI_API_KEY
```

### Ngrok не работает
```bash
# Проверить установку
ngrok version

# Проверить туннель
ngrok http 8080 --log=stdout
```

### Изображения не генерируются
```bash
# Проверить папку images
ls -la images/

# Проверить логи генерации
grep "🎨" server.log
```

## 🎉 Готово!

Теперь ваш AI сервер доступен из любой точки мира! 🎊

**Следующие шаги:**
1. Откройте публичный URL в браузере
2. Попробуйте сгенерировать изображение через веб-интерфейс
3. Протестируйте API для анализа кода
4. Поделитесь ссылкой с друзьями!

**Не забудьте:**
- Ngrok предоставляет временный URL (до перезапуска)
- Для постоянного доступа рассмотрите VPS
- Следите за использованием API (лимиты OpenAI)

---

## 📊 Мониторинг доступности

### Запуск сервиса мониторинга

```bash
# Управление монитором (рекомендуется)
./start_monitor.sh start   # запустить
./start_monitor.sh stop    # остановить
./start_monitor.sh status  # проверить статус
./start_monitor.sh logs    # посмотреть логи
./start_monitor.sh restart # перезапустить

# Или запуск напрямую
./monitor_service.sh

# Или в Docker (требует дополнительной настройки)
docker-compose -f docker-compose.monitor.yml up -d
```

### Что проверяет монитор:

✅ **Локальная доступность** - проверка сервиса на `http://localhost:8080`  
🌐 **Внешняя доступность** - проверка через ngrok туннель  
🔄 **Автоматическое обнаружение** - находит активные ngrok туннели  
⏰ **Регулярные ping-запросы** - поддерживает активность туннеля  

### Переменные окружения:

```bash
SERVICE_URL=http://host.docker.internal:8080          # Локальный URL сервиса
NGROK_API_URL=http://host.docker.internal:4040        # API ngrok для получения туннелей
INTERVAL=180                                          # Интервал проверки (сек)
EXTERNAL_CHECK_INTERVAL=300                          # Интервал внешней проверки (сек)
```

### Логи мониторинга:

```bash
docker logs ai-advent-monitor

# Пример вывода:
# ✅ Локальный сервис доступен
# ✅ Найден ngrok туннель: https://abc123.ngrok.io
# ✅ Внешний сервис доступен: https://abc123.ngrok.io
# ✅ Локальный ping отправлен успешно
# ✅ Внешний ping отправлен успешно
```

---

💡 **Совет:** Для постоянного домена зарегистрируйтесь на ngrok.com и получите кастомный субдомен!

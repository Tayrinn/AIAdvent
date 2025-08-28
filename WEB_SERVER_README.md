# 🚀 AI Code Analysis & Image Generation Web Server

Веб-сервис для автоматического анализа кода и генерации изображений с использованием ИИ, доступный извне.

## 📋 Возможности

- ✅ Анализ кода на наличие багов через OpenAI GPT
- ✅ Автоматическое исправление найденных проблем
- ✅ Генерация unit-тестов
- ✅ **🎨 Генерация изображений через Kandinsky AI**
- ✅ REST API для интеграции с другими системами
- ✅ CORS поддержка для веб-приложений
- ✅ Логирование всех запросов
- ✅ Веб-интерфейс для генерации изображений
- ✅ Статические файлы для изображений

## 🚀 Быстрый запуск

### 1. Сборка проекта
```bash
./gradlew build
```

### 2. Запуск сервера
```bash
./gradlew :shared:runWebServer
```

### 3. Проверка работы
```bash
# Проверка главной страницы
curl http://localhost:8080/

# Проверка веб-интерфейса генерации изображений
curl http://localhost:8080/generate

# Проверка API здоровья
curl http://localhost:8080/api/health
```

## 🌐 Настройка доступа извне

### Вариант 1: Ngrok (самый простой способ без доступа к роутеру) ⭐

#### Шаг 1: Установка Ngrok
```bash
# Скачайте ngrok для вашей ОС: https://ngrok.com/download
# Для macOS:
curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update && sudo apt install ngrok

# Для других ОС следуйте инструкциям на сайте ngrok.com
```

#### Шаг 2: Запуск tunneling
```bash
# Запустите ваш сервер в первом терминале
./gradlew :shared:runWebServer

# Во втором терминале создайте туннель
ngrok http 8080
```

#### Шаг 3: Получение публичного URL
После запуска ngrok вы увидите:
```
Forwarding    https://abc123.ngrok.io -> http://localhost:8080
Forwarding    http://abc123.ngrok.io -> http://localhost:8080
```

#### Шаг 4: Тестирование
```bash
# Используйте HTTPS URL для безопасности
curl https://abc123.ngrok.io/api/health
curl https://abc123.ngrok.io/generate
```

**Преимущества Ngrok:**
- ⚡ Настройка за 2 минуты
- 🔒 HTTPS по умолчанию
- 📊 Веб-интерфейс для мониторинга
- 🆓 Бесплатный тариф с ограничениями

### Вариант 2: LocalTunnel (бесплатная альтернатива)

#### Установка и запуск:
```bash
# Установите Node.js, затем:
npm install -g localtunnel

# Запустите tunneling
npx localtunnel --port 8080

# Или с кастомным субдоменом
npx localtunnel --port 8080 --subdomain my-ai-server
```

### Вариант 3: Cloudflare Tunnel (современное решение)

#### Шаг 1: Установка cloudflared
```bash
# Для macOS:
brew install cloudflare/cloudflare/cloudflared

# Для Linux:
curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared.deb
```

#### Шаг 2: Аутентификация
```bash
cloudflared tunnel login
```

#### Шаг 3: Создание туннеля
```bash
# Создайте туннель
cloudflared tunnel create my-ai-server

# Запустите туннель
cloudflared tunnel run my-ai-server --url http://localhost:8080
```

### Вариант 4: Serveo (SSH tunneling)

#### Простой SSH tunneling:
```bash
# SSH tunneling через Serveo
ssh -R 80:localhost:8080 serveo.net

# С кастомным субдоменом
ssh -R myserver:80:localhost:8080 serveo.net
```

### Вариант 5: Облачный сервер (VPS) - для постоянного доступа

Рекомендуемые провайдеры:
- **DigitalOcean** - от $6/месяц
- **Linode** - от $5/месяц
- **Hetzner** - от €3/месяц
- **AWS Lightsail** - от $3.5/месяц
- **Vultr** - от $2.5/месяц

#### Быстрый старт с DigitalOcean:
1. Зарегистрируйтесь на digitalocean.com
2. Создайте Droplet (Ubuntu 22.04, 1GB RAM)
3. Подключитесь по SSH
4. Установите Java и скопируйте ваш проект
5. Запустите сервер

## 📡 API Документация

### Базовый URL
```
http://your-server:8080
```

### Эндпоинты

#### GET /
Информация о сервере
```bash
curl http://localhost:8080/
```

#### GET /api/health
Проверка работоспособности
```bash
curl http://localhost:8080/api/health
```

#### GET /generate
Веб-интерфейс для генерации изображений
```bash
curl http://localhost:8080/generate
```

#### GET /images/{filename}
Получение сгенерированного изображения
```bash
curl http://localhost:8080/images/kandinsky_1234567890_123.png
```

#### POST /api/generate-image
Генерация изображений через Kandinsky AI

**Запрос:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "fun divide(a: Double, b: Double?): Double {\n    return a / b!!\n}",
    "fileName": "Calculator.kt",
    "language": "kotlin"
  }'
```

**Пример ответа:**
```json
{
  "success": true,
  "message": "Анализ завершен успешно",
  "bugs": [
    {
      "line": 2,
      "type": "unsafe_operator",
      "description": "Опасное использование !!",
      "severity": "high"
    }
  ],
  "fixedCode": "fun divide(a: Double, b: Double?): Double {\n    requireNotNull(b) { \"Parameter 'b' must not be null\" }\n    return a / b\n}",
  "tests": "import org.junit.Test\n\nclass CalculatorTest {\n    // Сгенерированные тесты...\n}"
}
```

**Пример запроса генерации изображений:**
```bash
curl -X POST http://localhost:8080/api/generate-image \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "красивый закат над горами, реалистичное фото",
    "style": "DEFAULT",
    "width": 1024,
    "height": 1024,
    "negativePrompt": "люди, текст, автомобили"
  }'
```

**Пример ответа генерации изображений:**
```json
{
  "success": true,
  "message": "Изображение успешно сгенерировано",
  "imageUrl": "/images/kandinsky_1703123456789_123456789.png",
  "fileName": "kandinsky_1703123456789_123456789.png"
}
```

## 🔒 Безопасность

### Важные рекомендации:

1. **Firewall**: Разрешите только порт 8080
   ```bash
   # Linux
   sudo ufw allow 8080

   # macOS
   # Используйте System Preferences > Security & Privacy > Firewall
   ```

2. **API Key**: Не передавайте API ключи в открытом виде
   - Используйте переменные окружения
   - Реализуйте аутентификацию

3. **Rate Limiting**: Добавьте ограничение запросов
4. **HTTPS**: Настройте SSL сертификат (Let's Encrypt)

### Переменные окружения:
```bash
export OPENAI_API_KEY="your-api-key-here"
export SERVER_PORT="8080"
export SERVER_HOST="0.0.0.0"
```

## 🐳 Docker (опционально)

### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем JAR файл
COPY build/libs/shared-1.0.0.jar app.jar

# Копируем config.properties
COPY config.properties .

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "com.tayrinn.aiadvent.server.AIAnalysisServerKt"]
```

### Сборка и запуск
```bash
# Сборка
docker build -t ai-analysis-server .

# Запуск
docker run -p 8080:8080 \
  -e OPENAI_API_KEY="your-key" \
  ai-analysis-server
```

## 🧪 Тестирование

### Простой тест:
```bash
# Проверка здоровья
curl http://localhost:8080/api/health

# Анализ кода
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code": "fun test() { println(\"Hello\") }"}'
```

### Тест с реальным кодом:
```bash
curl -X POST http://your-server:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d @test-data.json
```

Где `test-data.json`:
```json
{
  "code": "class Calculator {\n    fun divide(a: Int, b: Int): Int {\n        return a / b\n    }\n}",
  "fileName": "Calculator.kt"
}
```

## 📊 Мониторинг

### Логи сервера:
```bash
# Все логи
tail -f server.log

# Только ошибки
grep "ERROR" server.log
```

### Системные ресурсы:
```bash
# CPU и память
top

# Дисковое пространство
df -h

# Сетевые соединения
netstat -tlnp | grep :8080
```

## 🚨 Устранение неполадок

### Сервер не запускается:
1. Проверьте порт 8080: `lsof -i :8080`
2. Проверьте Java версию: `java -version`
3. Проверьте API ключ OpenAI

### Доступ извне не работает:
1. Проверьте firewall: `sudo ufw status`
2. Проверьте проброс портов в роутере
3. Проверьте внешний IP: `curl ifconfig.me`

### AI не отвечает:
1. Проверьте API ключ OpenAI
2. Проверьте лимиты использования
3. Проверьте подключение к интернету

## 📈 Производительность

### Оптимизация:
1. **JVM настройки**: `-Xmx2g -Xms512m`
2. **Connection pooling** для API запросов
3. **Кэширование** результатов анализа
4. **Асинхронная обработка** запросов

### Масштабирование:
1. **Load balancer** (nginx)
2. **Кластеризация** нескольких инстансов
3. **База данных** для кэширования
4. **Message queue** (RabbitMQ/Kafka)

## 🎯 Использование в продакшене

### Рекомендации:
1. Используйте **Docker** для контейнеризации
2. Настройте **monitoring** (Prometheus + Grafana)
3. Реализуйте **logging** (ELK stack)
4. Добавьте **metrics** и **health checks**
5. Настройте **CI/CD** пайплайн
6. Используйте **SSL/TLS** сертификаты

---

## 📞 Поддержка

Если возникли проблемы:
1. Проверьте логи сервера
2. Протестируйте локально: `curl http://localhost:8080/api/health`
3. Проверьте конфигурацию роутера
4. Убедитесь в корректности API ключа OpenAI

**Сервер готов к использованию! 🚀**

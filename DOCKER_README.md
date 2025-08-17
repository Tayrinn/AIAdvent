# 🐱 Daily Cat Generator Service

Автоматический сервис для ежедневной генерации изображений котиков через Kandinsky API с отправкой отчетов на email.

## 🚀 Возможности

- **🤖 Автоматическая генерация** котиков каждый вечер в 20:00
- **📧 Email уведомления** с изображением и статистикой
- **📊 Отслеживание лимитов** API (осталось/всего генераций)
- **🐳 Docker контейнеры** для простого развертывания
- **📝 Подробные логи** всех операций

## 🛠️ Требования

- Docker и Docker Compose
- Gmail аккаунт с App Password
- Kandinsky API ключи от Segmind

## ⚙️ Настройка

### 1. Настройка Gmail для отправки email

1. **Включите 2FA** в вашем Gmail аккаунте
2. **Создайте App Password:**
   - Перейдите в [Google Account Settings](https://myaccount.google.com/)
   - Security → 2-Step Verification → App passwords
   - Создайте пароль для "Mail"
3. **Скопируйте пароль** (16 символов)

### 2. Настройка .env файла

```bash
# Скопируйте .env.example
cp .env.example .env

# Отредактируйте .env файл
nano .env
```

**Обязательные настройки:**
```env
# Kandinsky API Keys
KANDINSKY_API_KEY=your_api_key_here
KANDINSKY_SECRET_KEY=your_secret_key_here

# Email Configuration (Gmail)
GMAIL_USERNAME=your_email@gmail.com
GMAIL_APP_PASSWORD=your_16_char_app_password

# API Limits
REMAINING_GENERATIONS=91
TOTAL_GENERATIONS=100
```

## 🐳 Запуск

### Быстрый запуск

```bash
# Запустить все сервисы
./run_cat_service.sh start

# Проверить статус
./run_cat_service.sh status

# Протестировать генерацию
./run_cat_service.sh test
```

### Ручной запуск

```bash
# Собрать и запустить контейнеры
docker-compose up -d --build

# Просмотр логов
docker-compose logs -f

# Остановка сервисов
docker-compose down
```

## 📋 Команды управления

```bash
./run_cat_service.sh start     # Запустить сервисы
./run_cat_service.sh stop      # Остановить сервисы
./run_cat_service.sh restart   # Перезапустить сервисы
./run_cat_service.sh status    # Показать статус
./run_cat_service.sh test      # Протестировать генерацию
./run_cat_service.sh logs      # Показать логи
./run_cat_service.sh reset     # Сбросить счетчик генераций
./run_cat_service.sh help      # Показать справку
```

## 🏗️ Архитектура

### Сервисы

1. **mcp-server** - MCP сервер для генерации изображений
   - Порт: 8000
   - API: `/generate`, `/health`
   - Использует FusionBrain API (Kandinsky)

2. **daily-cat-generator** - Ежедневный генератор котиков
   - Cron задача: каждый день в 20:00
   - Генерирует изображение котика
   - Отправляет отчет на email
   - Обновляет счетчик лимитов

### Схема работы

```
20:00 каждый день
    ↓
daily-cat-generator
    ↓
POST /generate → mcp-server
    ↓
FusionBrain API (Kandinsky)
    ↓
Скачивание изображения
    ↓
Отправка email отчета
    ↓
Обновление счетчика лимитов
```

## 📧 Email отчеты

### Структура отчета

- **Тема:** 🐱 Ежедневный котик - ДД.ММ.ГГГГ
- **Содержание:**
  - Дата и время генерации
  - Использованный промпт
  - Оставшиеся генерации
  - Статистика использования
  - Сгенерированное изображение

### Пример отчета

```
🐱 Ежедневный котик готов!

Дата: 15.08.2025 20:00
Промпт: adorable fluffy cat with big eyes, sitting in a cozy basket
Осталось генераций: 90/100
Использовано: 10

📊 Статистика:
• Всего генераций: 100
• Осталось: 90
• Процент использования: 10.0%
```

## 🔧 Мониторинг и логи

### Логи

- **MCP сервер:** `docker-compose logs mcp-server`
- **Генератор котиков:** `docker-compose logs daily-cat-generator`
- **Все логи:** `docker-compose logs -f`

### Статус сервисов

```bash
# Проверить статус контейнеров
docker-compose ps

# Проверить здоровье MCP сервера
curl http://localhost:8000/health
```

## 🚨 Устранение проблем

### Проблема: Email не отправляется

1. **Проверьте Gmail настройки:**
   - 2FA включен
   - App Password создан правильно
   - Username указан без @gmail.com

2. **Проверьте логи:**
   ```bash
   docker-compose logs daily-cat-generator
   ```

### Проблема: Генерация изображений не работает

1. **Проверьте API ключи:**
   - Kandinsky API ключи корректны
   - Лимит не исчерпан

2. **Проверьте MCP сервер:**
   ```bash
   curl http://localhost:8000/health
   ```

### Проблема: Cron не запускается

1. **Проверьте время запуска:**
   - По умолчанию: 20:00 (8 PM)
   - Измените в Dockerfile.daily

2. **Проверьте логи cron:**
   ```bash
   docker-compose exec daily-cat-generator cat /var/log/cron.log
   ```

## 📁 Структура файлов

```
.
├── Dockerfile                 # MCP сервер
├── Dockerfile.daily          # Ежедневный генератор
├── docker-compose.yml        # Конфигурация сервисов
├── daily_cat_generator.py    # Скрипт генерации котиков
├── mcp_server.py             # MCP сервер
├── requirements.txt           # Python зависимости
├── run_cat_service.sh        # Скрипт управления
├── .env                      # Конфигурация (не коммитится)
├── .env.example              # Пример конфигурации
├── images/                   # Сгенерированные изображения
└── logs/                     # Логи сервисов
```

## 🔄 Обновление

```bash
# Остановить сервисы
./run_cat_service.sh stop

# Обновить код
git pull

# Перезапустить с новой версией
./run_cat_service.sh start
```

## 📝 Лицензия

MIT License

## 🤝 Поддержка

При возникновении проблем:

1. Проверьте логи: `./run_cat_service.sh logs`
2. Проверьте статус: `./run_cat_service.sh status`
3. Создайте Issue в GitHub с описанием проблемы

---

**🐾 Наслаждайтесь ежедневными котиками!** 🐱✨

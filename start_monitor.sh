#!/bin/bash

# Скрипт для запуска сервиса-монитора в Docker
# Поддерживает активность ngrok туннеля путем регулярных запросов

echo "🚀 Запуск сервиса-монитора для поддержания активности ngrok"
echo ""

# Проверяем, что Docker установлен
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не установлен. Установите Docker для запуска монитора."
    exit 1
fi

# Проверяем, что docker-compose установлен
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose не установлен. Установите Docker Compose."
    exit 1
fi

# Проверяем, что веб-сервер запущен
echo "🔍 Проверяем доступность веб-сервера..."
if ! curl -s -f http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "⚠️  Веб-сервер не запущен на порту 8080"
    echo "💡 Сначала запустите веб-сервер командой:"
    echo "   source venv/bin/activate && ./gradlew :shared:runWebServer"
    echo ""
    echo "❓ Продолжить запуск монитора? (y/N): "
    read -r response
    if [[ ! "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo "Отмена запуска."
        exit 0
    fi
else
    echo "✅ Веб-сервер доступен"
fi

echo ""
echo "🐳 Запускаем Docker контейнер с монитором..."

# Запускаем сервис
if command -v docker-compose &> /dev/null; then
    docker-compose -f docker-compose.monitor.yml up -d
else
    docker compose -f docker-compose.monitor.yml up -d
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Сервис-монитор успешно запущен!"
    echo ""
    echo "📊 Статус контейнера:"
    docker ps | grep ai-advent-monitor
    echo ""
    echo "📝 Логи монитора:"
    echo "   docker logs -f ai-advent-monitor"
    echo ""
    echo "🛑 Остановка монитора:"
    echo "   docker stop ai-advent-monitor"
    echo ""
    echo "🎯 Монитор будет:"
    echo "   • Проверять доступность сервиса каждые 3 минуты"
    echo "   • Генерировать тестовые изображения для поддержания активности"
    echo "   • Автоматически перезапускаться при сбоях"
else
    echo ""
    echo "❌ Ошибка запуска сервиса-монитора"
    exit 1
fi

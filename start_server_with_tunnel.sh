#!/bin/bash

echo "🚀 Запуск AI Server с Ngrok туннелем"
echo "===================================="

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функция для проверки команд
check_command() {
    if command -v $1 &> /dev/null; then
        return 0
    else
        echo -e "${RED}❌ $1 не найден. Установите его сначала.${NC}"
        return 1
    fi
}

# Проверка наличия необходимых команд
echo "🔍 Проверка зависимостей..."
check_command java || exit 1
check_command ngrok || {
    echo -e "${YELLOW}⚠️  Ngrok не найден. Установите его:${NC}"
    echo "   ./setup_ngrok.sh"
    exit 1
}

echo -e "${GREEN}✅ Все зависимости установлены${NC}"
echo ""

# Создание папки для изображений
echo "📁 Создание папки для изображений..."
mkdir -p images
echo -e "${GREEN}✅ Папка images создана${NC}"
echo ""

# Функция для запуска сервера
start_server() {
    echo -e "${BLUE}🔧 Запуск AI Code Analysis Server...${NC}"
    echo "   Порт: 8080"
    echo "   Локальный URL: http://localhost:8080"
    echo ""

    # Запуск сервера
    ./gradlew :shared:runWebServer
}

# Функция для запуска ngrok
start_ngrok() {
    echo -e "${BLUE}🌐 Запуск Ngrok туннеля...${NC}"
    echo "   Порт: 8080"
    echo "   Ожидание публичного URL..."
    echo ""

    # Небольшая задержка для запуска сервера
    sleep 3

    # Запуск ngrok
    ngrok http 8080
}

# Проверка режима запуска
if [ "$1" = "--server" ]; then
    start_server
elif [ "$1" = "--tunnel" ]; then
    start_ngrok
else
    echo "📋 Инструкция по запуску:"
    echo ""
    echo "Вариант 1 - Автоматический запуск (рекомендуется):"
    echo ""
    echo "1. Откройте первый терминал и запустите сервер:"
    echo "   ./start_server_with_tunnel.sh --server"
    echo ""
    echo "2. Откройте второй терминал и запустите туннель:"
    echo "   ./start_server_with_tunnel.sh --tunnel"
    echo ""
    echo "3. Используйте HTTPS URL из ngrok для доступа извне"
    echo ""
    echo "Вариант 2 - Ручной запуск:"
    echo ""
    echo "1. В первом терминале:"
    echo "   ./gradlew :shared:runWebServer"
    echo ""
    echo "2. Во втором терминале:"
    echo "   ngrok http 8080"
    echo ""
    echo "Вариант 3 - С кастомным субдоменом:"
    echo "   ngrok http 8080 --subdomain=my-ai-server"
    echo ""
    echo "💡 Полезные команды ngrok:"
    echo "   • ngrok http 8080 --region=eu     # Европейский регион"
    echo "   • ngrok http 8080 --region=us     # Американский регион"
    echo "   • ngrok http 8080 --region=ap     # Азиатско-тихоокеанский"
    echo "   • ngrok dashboard                 # Веб-интерфейс ngrok"
    echo ""
    echo "🔗 После запуска вы получите HTTPS URL вида:"
    echo "   https://abc123.ngrok.io"
    echo ""
    echo "📱 Доступные страницы:"
    echo "   • Главная: https://abc123.ngrok.io/"
    echo "   • Веб-интерфейс генерации: https://abc123.ngrok.io/generate"
    echo "   • API: https://abc123.ngrok.io/api/health"
    echo ""
fi

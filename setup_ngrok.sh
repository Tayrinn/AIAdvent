#!/bin/bash

echo "🚀 Настройка Ngrok для AI Code Analysis Server"
echo "=============================================="

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функция для проверки установки
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✅ $1 установлен${NC}"
        return 0
    else
        echo -e "${RED}❌ $1 не найден${NC}"
        return 1
    fi
}

# Функция для определения ОС
detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "linux";;
        Darwin*)    echo "macos";;
        CYGWIN*|MINGW*|MSYS*) echo "windows";;
        *)          echo "unknown";;
    esac
}

# Функция для определения архитектуры
detect_arch() {
    case "$(uname -m)" in
        x86_64*)    echo "amd64";;
        i386*)      echo "386";;
        arm64*)     echo "arm64";;
        aarch64*)   echo "arm64";;
        arm*)       echo "arm";;
        *)          echo "unknown";;
    esac
}

echo "🔍 Определение системы..."
OS=$(detect_os)
ARCH=$(detect_arch)
echo "   ОС: $OS"
echo "   Архитектура: $ARCH"

# Проверка наличия curl или wget
if command -v curl &> /dev/null; then
    DOWNLOADER="curl -L -o"
elif command -v wget &> /dev/null; then
    DOWNLOADER="wget -O"
else
    echo -e "${RED}❌ Не найден curl или wget для загрузки ngrok${NC}"
    exit 1
fi

echo ""
echo "📦 Установка ngrok..."

# Скачивание ngrok
NGROK_URL=""
case $OS in
    "linux")
        NGROK_URL="https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-$ARCH.tgz"
        ;;
    "macos")
        NGROK_URL="https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-darwin-$ARCH.tgz"
        ;;
    "windows")
        NGROK_URL="https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-windows-$ARCH.zip"
        ;;
    *)
        echo -e "${RED}❌ Неподдерживаемая ОС: $OS${NC}"
        exit 1
        ;;
esac

if [ -z "$NGROK_URL" ]; then
    echo -e "${RED}❌ Не удалось определить URL для загрузки ngrok${NC}"
    exit 1
fi

echo "   Скачивание ngrok..."
$DOWNLOADER ngrok.tgz "$NGROK_URL"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Ошибка загрузки ngrok${NC}"
    exit 1
fi

echo "   Распаковка..."
tar -xzf ngrok.tgz

if [ ! -f "ngrok" ]; then
    echo -e "${RED}❌ Ошибка распаковки ngrok${NC}"
    exit 1
fi

# Перемещение в /usr/local/bin (если есть права)
if [ -w "/usr/local/bin" ] || sudo -n true 2>/dev/null; then
    echo "   Установка в /usr/local/bin..."
    if sudo mv ngrok /usr/local/bin/ 2>/dev/null; then
        echo -e "${GREEN}✅ ngrok установлен глобально${NC}"
    else
        echo -e "${YELLOW}⚠️  Нет прав на установку в /usr/local/bin${NC}"
        chmod +x ngrok
        echo -e "${YELLOW}   ngrok оставлен в текущей директории${NC}"
    fi
else
    chmod +x ngrok
    echo -e "${YELLOW}⚠️  ngrok оставлен в текущей директории${NC}"
fi

# Очистка
rm -f ngrok.tgz

echo ""
echo "🔧 Проверка установки..."
if check_command ngrok; then
    echo ""
    echo "🎉 Ngrok успешно установлен!"
    echo ""
    echo "📋 Следующие шаги:"
    echo ""
    echo "1. Запустите ваш AI сервер:"
    echo "   ./gradlew :shared:runWebServer"
    echo ""
    echo "2. В новом терминале создайте туннель:"
    echo "   ngrok http 8080"
    echo ""
    echo "3. Используйте полученный HTTPS URL для доступа извне"
    echo ""
    echo "💡 Полезные команды:"
    echo "   • ngrok http 8080              # Создать туннель"
    echo "   • ngrok http 8080 --subdomain=myapp  # С кастомным субдоменом"
    echo "   • ngrok http 8080 --region=eu   # Выбрать регион"
    echo "   • ngrok dashboard              # Открыть веб-интерфейс"
    echo ""
    echo "🔗 Регистрация на ngrok.com для дополнительных возможностей:"
    echo "   - Кастомные домены"
    echo "   - Постоянные URL"
    echo "   - Увеличенные лимиты"
    echo ""
else
    echo -e "${RED}❌ Установка ngrok не удалась${NC}"
    exit 1
fi

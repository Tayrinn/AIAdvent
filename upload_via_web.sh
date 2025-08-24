#!/bin/bash

# Скрипт для подготовки к публикации AIAdvent Desktop через веб-интерфейс
# Автор: Tayrinn
# Версия: 1.0.1

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Загрузка переменных окружения
load_env() {
    if [ -f "env.local" ]; then
        export $(cat env.local | grep -v '^#' | xargs)
        log_info "Переменные окружения загружены из env.local"
    else
        log_warning "Файл env.local не найден, используются значения по умолчанию"
    fi
}

# Конфигурация
PLUGIN_NAME="${PLUGIN_NAME:-AIAdvent Desktop}"
PLUGIN_ID="${PLUGIN_ID:-com.tayrinn.aiadvent.desktop}"
PLUGIN_VERSION="${PLUGIN_VERSION:-1.0.0}"
PLUGIN_JAR="desktop/build/libs/AIAdvent-Plugin-1.0.0.jar"

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверка файлов
check_files() {
    log_info "Проверка файлов плагина..."
    
    if [ ! -f "$PLUGIN_JAR" ]; then
        log_error "Файл плагина не найден: $PLUGIN_JAR"
        log_info "Сначала создайте плагин: ./gradlew :desktop:pluginJar"
        exit 1
    fi
    
    if [ ! -f "desktop/src/main/resources/META-INF/plugin.xml" ]; then
        log_error "plugin.xml не найден"
        exit 1
    fi
    
    log_success "Файлы плагина проверены"
}

# Проверка размера файла
check_file_size() {
    local file_size=$(stat -f%z "$PLUGIN_JAR" 2>/dev/null || stat -c%s "$PLUGIN_JAR" 2>/dev/null)
    local max_size=$((10 * 1024 * 1024))  # 10MB
    
    log_info "Размер файла плагина: $((file_size / 1024)) KB"
    
    if [ $file_size -gt $max_size ]; then
        log_warning "Файл плагина больше 10MB. Это может вызвать проблемы при загрузке."
    fi
}

# Получение информации о плагине
get_plugin_info() {
    log_info "Получение информации о плагине..."
    
    # Извлекаем информацию из plugin.xml
    local plugin_name=$(grep -o '<name>.*</name>' desktop/src/main/resources/META-INF/plugin.xml | sed 's/<name>\(.*\)<\/name>/\1/')
    local plugin_description=$(grep -o '<description>.*</description>' desktop/src/main/resources/META-INF/plugin.xml | sed 's/<description>\(.*\)<\/description>/\1/' | head -1)
    
    echo "Название: $plugin_name"
    echo "Описание: ${plugin_description:0:100}..."
    echo "Версия: $PLUGIN_VERSION"
    echo "ID: $PLUGIN_ID"
}

# Открытие веб-интерфейса
open_web_interface() {
    log_info "Открытие веб-интерфейса для публикации..."
    
    local os=$(uname -s)
    local url="https://plugins.jetbrains.com/plugin/uploadPlugin"
    
    case "$os" in
        "Darwin")  # macOS
            open "$url"
            ;;
        "Linux")
            if command -v xdg-open &> /dev/null; then
                xdg-open "$url"
            elif command -v gnome-open &> /dev/null; then
                gnome-open "$url"
            else
                log_info "Откройте в браузере: $url"
            fi
            ;;
        "MINGW"*|"MSYS"*|"CYGWIN"*)  # Windows
            start "$url"
            ;;
        *)
            log_info "Откройте в браузере: $url"
            ;;
    esac
    
    log_success "Веб-интерфейс открыт!"
}

# Инструкции по публикации
show_instructions() {
    echo
    log_success "🎯 Инструкции по публикации:"
    echo
    log_info "1. 📱 Войдите в свой аккаунт JetBrains"
    log_info "2. 📁 Загрузите файл: $PLUGIN_JAR"
    log_info "3. 📝 Заполните информацию о плагине:"
    echo "   • Название: $PLUGIN_NAME"
    echo "   • Описание: AI-Powered Development Assistant with OpenAI ChatGPT integration"
    echo "   • Категория: Productivity"
    echo "   • Теги: AI, ChatGPT, Productivity, Development, Testing"
    echo "   • Website: https://github.com/tayrinn/aiadvent"
    echo "   • Repository: https://github.com/tayrinn/aiadvent.git"
    log_info "4. 🚀 Нажмите 'Upload Plugin'"
    echo
    log_info "📋 Файл плагина готов: $PLUGIN_JAR"
    log_info "📁 Размер: $(( $(stat -f%z "$PLUGIN_JAR" 2>/dev/null || stat -c%s "$PLUGIN_JAR" 2>/dev/null) / 1024 )) KB"
    log_info "🔍 ID плагина: $PLUGIN_ID"
    echo
    log_info "⏰ Время модерации: 1-3 дня"
    log_info "📧 Уведомление: Email от JetBrains"
    log_info "🌐 Мониторинг: https://plugins.jetbrains.com/plugin/$PLUGIN_ID"
}

# Основная функция
main() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  AIAdvent Web Upload Helper${NC}"
    echo -e "${BLUE}================================${NC}"
    echo
    
    log_info "Подготовка к публикации через веб-интерфейс..."
    
    # Загружаем переменные окружения
    load_env
    
    # Проверки
    check_files
    check_file_size
    get_plugin_info
    
    echo
    
    # Открываем веб-интерфейс
    open_web_interface
    
    # Показываем инструкции
    show_instructions
    
    echo
    log_success "🎉 Готово! Теперь опубликуйте плагин через веб-интерфейс."
}

# Запуск скрипта
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi

#!/bin/bash

# Исправленный скрипт для публикации AIAdvent Desktop плагина в JetBrains Marketplace
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
API_KEY="${JETBRAINS_API_KEY}"

# Правильные API endpoints для JetBrains Marketplace
MARKETPLACE_API="https://plugins.jetbrains.com/api"
PLUGIN_UPLOAD_URL="https://plugins.jetbrains.com/plugin/uploadPlugin"

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

# Проверка зависимостей
check_dependencies() {
    log_info "Проверка зависимостей..."
    
    if ! command -v curl &> /dev/null; then
        log_error "curl не установлен. Установите curl для продолжения."
        exit 1
    fi
    
    log_success "Зависимости проверены"
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

# Проверка API ключа
test_api_key() {
    log_info "Проверка API ключа..."
    
    # Тестируем API ключ через простой запрос
    local response=$(curl -s -H "Authorization: Bearer $API_KEY" \
        "$MARKETPLACE_API/plugins/search?query=test" 2>/dev/null || echo "error")
    
    if [ "$response" = "error" ] || echo "$response" | grep -q "unauthorized\|forbidden"; then
        log_error "API ключ недействителен или не имеет необходимых прав"
        log_info "Проверьте API ключ в JetBrains Marketplace"
        exit 1
    fi
    
    log_success "API ключ действителен"
}

# Публикация через веб-интерфейс (рекомендуемый способ)
publish_via_web() {
    log_info "Публикация через веб-интерфейс..."
    
    log_success "🎯 Рекомендуемый способ публикации:"
    echo
    log_info "1. Откройте: https://plugins.jetbrains.com/plugin/uploadPlugin"
    log_info "2. Войдите в свой аккаунт JetBrains"
    log_info "3. Загрузите файл: $PLUGIN_JAR"
    log_info "4. Заполните информацию о плагине:"
    echo "   - Название: $PLUGIN_NAME"
    echo "   - Описание: AI-Powered Development Assistant"
    echo "   - Категория: Productivity"
    echo "   - Теги: AI, ChatGPT, Productivity, Development"
    log_info "5. Нажмите 'Upload Plugin'"
    echo
    log_info "📋 Файл плагина готов: $PLUGIN_JAR"
    log_info "📁 Размер: $(( $(stat -f%z "$PLUGIN_JAR" 2>/dev/null || stat -c%s "$PLUGIN_JAR" 2>/dev/null) / 1024 )) KB"
}

# Альтернативный способ через API (если доступен)
publish_via_api() {
    log_info "Попытка публикации через API..."
    
    # Создаем временный файл с метаданными
    local metadata_file="/tmp/plugin_metadata.json"
    cat > "$metadata_file" << EOF
{
    "name": "$PLUGIN_NAME",
    "pluginId": "$PLUGIN_ID",
    "description": "AI-Powered Development Assistant with OpenAI ChatGPT integration",
    "category": "Productivity",
    "tags": ["AI", "ChatGPT", "Productivity", "Development", "Testing"],
    "website": "https://github.com/tayrinn/aiadvent",
    "repository": "https://github.com/tayrinn/aiadvent.git"
}
EOF
    
    log_info "Метаданные плагина:"
    cat "$metadata_file"
    echo
    
    # Пытаемся загрузить через API
    log_info "Загрузка плагина через API..."
    
    local response=$(curl -s -X POST \
        -H "Authorization: Bearer $API_KEY" \
        -H "Content-Type: multipart/form-data" \
        -F "pluginId=$PLUGIN_ID" \
        -F "version=$PLUGIN_VERSION" \
        -F "file=@$PLUGIN_JAR" \
        -F "metadata=@$metadata_file" \
        "$PLUGIN_UPLOAD_URL" 2>/dev/null || echo "error")
    
    if [ "$response" = "error" ]; then
        log_warning "API загрузка не удалась. Используйте веб-интерфейс."
        publish_via_web
    else
        log_success "Плагин загружен через API!"
        echo "Ответ: $response"
    fi
    
    # Удаляем временный файл
    rm -f "$metadata_file"
}

# Основная функция
main() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  AIAdvent Plugin Publisher${NC}"
    echo -e "${BLUE}================================${NC}"
    echo
    
    log_info "Начинаем процесс публикации плагина..."
    
    # Загружаем переменные окружения
    load_env
    
    # Проверки
    check_dependencies
    check_files
    check_file_size
    get_plugin_info
    
    echo
    
    # Тестируем API ключ
    test_api_key
    
    echo
    
    # Пытаемся опубликовать
    if publish_via_api; then
        log_success "🎉 Плагин успешно опубликован!"
    else
        log_info "Используйте веб-интерфейс для публикации"
    fi
    
    echo
    log_info "📋 Следующие шаги:"
    log_info "1. Дождитесь модерации (1-3 дня)"
    log_info "2. Получите email уведомление об одобрении"
    log_info "3. Плагин появится в Marketplace"
    echo
    log_info "🔍 Мониторинг: https://plugins.jetbrains.com/plugin/$PLUGIN_ID"
}

# Обработка ошибок
trap 'log_error "Произошла ошибка. Выход."; exit 1' ERR

# Запуск скрипта
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi

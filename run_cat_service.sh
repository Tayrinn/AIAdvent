#!/bin/bash

# Daily Cat Generator Service Manager
# Управление сервисом для ежедневной генерации котиков

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функции
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверяем наличие Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker не установлен. Установите Docker и попробуйте снова."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose не установлен. Установите Docker Compose и попробуйте снова."
        exit 1
    fi
    
    print_success "Docker и Docker Compose найдены"
}

# Проверяем .env файл
check_env() {
    if [ ! -f .env ]; then
        print_error "Файл .env не найден. Создайте его на основе .env.example"
        exit 1
    fi
    
    # Проверяем обязательные переменные
    source .env
    
    if [ -z "$KANDINSKY_API_KEY" ] || [ -z "$KANDINSKY_SECRET_KEY" ]; then
        print_error "Не настроены API ключи Kandinsky в .env файле"
        exit 1
    fi
    
    if [ -z "$GMAIL_USERNAME" ] || [ "$GMAIL_USERNAME" = "your_email@gmail.com" ]; then
        print_warning "Не настроен Gmail username. Email уведомления не будут работать"
    fi
    
    if [ -z "$GMAIL_APP_PASSWORD" ] || [ "$GMAIL_APP_PASSWORD" = "your_app_password_here" ]; then
        print_warning "Не настроен Gmail app password. Email уведомления не будут работать"
    fi
    
    print_success "Файл .env проверен"
}

# Создаем необходимые папки
create_directories() {
    mkdir -p images
    mkdir -p logs
    print_success "Созданы необходимые папки"
}

# Запускаем сервисы
start_services() {
    print_info "Запускаю сервисы..."
    docker-compose up -d --build
    
    print_info "Ожидаю запуска MCP сервера..."
    sleep 10
    
    # Проверяем здоровье сервера
    if curl -f http://localhost:8000/health > /dev/null 2>&1; then
        print_success "MCP сервер запущен и работает"
    else
        print_error "MCP сервер не отвечает"
        docker-compose logs mcp-server
        exit 1
    fi
    
    print_success "Все сервисы запущены успешно!"
}

# Останавливаем сервисы
stop_services() {
    print_info "Останавливаю сервисы..."
    docker-compose down
    print_success "Сервисы остановлены"
}

# Показываем статус
show_status() {
    print_info "Статус сервисов:"
    docker-compose ps
    
    print_info "Логи MCP сервера:"
    docker-compose logs --tail=20 mcp-server
    
    print_info "Логи генератора котиков:"
    docker-compose logs --tail=20 daily-cat-generator
}

# Тестируем генерацию
test_generation() {
    print_info "Тестирую генерацию изображения..."
    
    # Ждем запуска сервера
    sleep 5
    
    # Отправляем тестовый запрос
    response=$(curl -s -X POST http://localhost:8000/generate \
        -H "Content-Type: application/json" \
        -d '{
            "type": "GENERATE",
            "style": "DEFAULT",
            "width": 512,
            "height": 512,
            "numImages": 1,
            "generateParams": {
                "query": "test cat image"
            }
        }')
    
    if echo "$response" | grep -q "imageUrl"; then
        print_success "Тестовая генерация прошла успешно!"
        echo "Ответ: $response"
    else
        print_error "Тестовая генерация не удалась"
        echo "Ответ: $response"
    fi
}

# Показываем логи
show_logs() {
    print_info "Показываю логи..."
    docker-compose logs -f
}

# Сбрасываем счетчик генераций
reset_counter() {
    print_info "Сбрасываю счетчик генераций..."
    
    # Обновляем .env файл
    sed -i.bak 's/REMAINING_GENERATIONS=.*/REMAINING_GENERATIONS=91/' .env
    sed -i.bak 's/TOTAL_GENERATIONS=.*/TOTAL_GENERATIONS=100/' .env
    
    print_success "Счетчик сброшен: 91/100"
}

# Показываем справку
show_help() {
    echo "Daily Cat Generator Service Manager"
    echo ""
    echo "Использование: $0 [команда]"
    echo ""
    echo "Команды:"
    echo "  start     - Запустить все сервисы"
    echo "  stop      - Остановить все сервисы"
    echo "  restart   - Перезапустить сервисы"
    echo "  status    - Показать статус сервисов"
    echo "  test      - Протестировать генерацию"
    echo "  logs      - Показать логи"
    echo "  reset     - Сбросить счетчик генераций"
    echo "  help      - Показать эту справку"
    echo ""
    echo "Примеры:"
    echo "  $0 start    # Запустить сервисы"
    echo "  $0 status   # Показать статус"
    echo "  $0 test     # Протестировать генерацию"
}

# Основная логика
main() {
    case "${1:-help}" in
        start)
            check_docker
            check_env
            create_directories
            start_services
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            sleep 2
            start_services
            ;;
        status)
            show_status
            ;;
        test)
            test_generation
            ;;
        logs)
            show_logs
            ;;
        reset)
            reset_counter
            ;;
        help|*)
            show_help
            ;;
    esac
}

# Запускаем основную функцию
main "$@"

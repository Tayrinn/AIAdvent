#!/bin/sh

# Сервис-монитор для поддержания активности ngrok туннеля
# Делает запросы к веб-серверу каждые 3 минуты

# Конфигурация
SERVICE_URL="${SERVICE_URL:-http://host.docker.internal:8080}"
NGROK_API_URL="${NGROK_API_URL:-http://localhost:4040}"
INTERVAL="${INTERVAL:-180}"  # 3 минуты в секундах
HEALTH_ENDPOINT="/api/health"
ROOT_ENDPOINT="/"
EXTERNAL_CHECK_INTERVAL="${EXTERNAL_CHECK_INTERVAL:-300}"  # 5 минут для проверки внешнего доступа

echo "🚀 Запуск сервиса-монитора доступности"
echo "📍 URL сервиса: $SERVICE_URL"
echo "🌐 Ngrok API: $NGROK_API_URL"
echo "⏱️  Интервал проверки: $INTERVAL секунд"
echo "🌍 Внешняя проверка: $EXTERNAL_CHECK_INTERVAL секунд"
echo "🏥 Health endpoint: $HEALTH_ENDPOINT"
echo "🏓 Ping endpoint: $ROOT_ENDPOINT"
echo ""

# Глобальные переменные для отслеживания состояния
LAST_EXTERNAL_CHECK=0
NGROK_URL=""
NGROK_AVAILABLE=false

# Функция получения URL ngrok туннеля
get_ngrok_url() {
    # Проверяем, запущен ли ngrok
    if ! curl -s "$NGROK_API_URL/api/tunnels" > /dev/null 2>&1; then
        echo "❌ Ngrok API недоступен"
        NGROK_AVAILABLE=false
        return 1
    fi

    # Получаем список туннелей
    TUNNELS_JSON=$(curl -s "$NGROK_API_URL/api/tunnels" 2>/dev/null)

    if [ $? -ne 0 ] || [ -z "$TUNNELS_JSON" ]; then
        echo "❌ Не удалось получить данные от ngrok API"
        NGROK_AVAILABLE=false
        return 1
    fi

    # Извлекаем публичный URL (HTTPS)
    PUBLIC_URL=$(echo "$TUNNELS_JSON" | grep -o '"public_url":"[^"]*"' | grep https | head -1 | sed 's/"public_url":"//;s/"//')

    if [ -z "$PUBLIC_URL" ]; then
        echo "❌ Не найден HTTPS туннель ngrok"
        NGROK_AVAILABLE=false
        return 1
    fi

    NGROK_URL="$PUBLIC_URL"
    NGROK_AVAILABLE=true
    echo "✅ Найден ngrok туннель: $NGROK_URL"
    return 0
}

# Функция проверки доступности локального сервиса
check_local_service() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - 🔍 Проверяем локальную доступность сервиса..."

    # Проверяем health endpoint
    if curl -s -f "$SERVICE_URL$HEALTH_ENDPOINT" > /dev/null 2>&1; then
        echo "✅ Локальный сервис доступен"
        return 0
    else
        echo "❌ Локальный сервис недоступен"
        return 1
    fi
}

# Функция проверки доступности внешнего сервиса через ngrok
check_external_service() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - 🌍 Проверяем внешнюю доступность через ngrok..."

    # Получаем текущий URL ngrok
    if ! get_ngrok_url; then
        echo "⚠️  Ngrok недоступен, пропускаем внешнюю проверку"
        return 1
    fi

    # Проверяем внешний health endpoint
    if curl -s -f "$NGROK_URL$HEALTH_ENDPOINT" > /dev/null 2>&1; then
        echo "✅ Внешний сервис доступен: $NGROK_URL"
        return 0
    else
        echo "❌ Внешний сервис недоступен: $NGROK_URL"
        return 1
    fi
}

# Функция проверки доступности (комбинированная)
check_service() {
    local current_time=$(date +%s)
    local should_check_external=false

    # Определяем, нужно ли проверять внешний доступ
    if [ $((current_time - LAST_EXTERNAL_CHECK)) -ge $EXTERNAL_CHECK_INTERVAL ]; then
        should_check_external=true
        LAST_EXTERNAL_CHECK=$current_time
    fi

    local local_ok=false
    local external_ok=false

    # Всегда проверяем локальный сервис
    if check_local_service; then
        local_ok=true
    fi

    # Проверяем внешний доступ по расписанию
    if [ "$should_check_external" = true ]; then
        if check_external_service; then
            external_ok=true
        fi
    else
        echo "$(date '+%Y-%m-%d %H:%M:%S') - ⏰ Следующая внешняя проверка через $((EXTERNAL_CHECK_INTERVAL - (current_time - LAST_EXTERNAL_CHECK))) сек"
    fi

    # Возвращаем успех, если хотя бы один из сервисов доступен
    if [ "$local_ok" = true ] || [ "$external_ok" = true ]; then
        return 0
    else
        return 1
    fi
}

# Функция отправки ping запросов (локальный и внешний)
send_ping_request() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - 🏓 Отправляем ping запросы для поддержания активности..."

    local ping_success=false

    # Всегда отправляем ping на локальный сервис
    if curl -s -f "$SERVICE_URL$ROOT_ENDPOINT" > /dev/null 2>&1; then
        echo "✅ Локальный ping отправлен успешно"
        ping_success=true
    else
        echo "❌ Ошибка локального ping запроса"
    fi

    # Если ngrok доступен, отправляем ping и на внешний адрес
    if [ "$NGROK_AVAILABLE" = true ] && [ -n "$NGROK_URL" ]; then
        if curl -s -f "$NGROK_URL$ROOT_ENDPOINT" > /dev/null 2>&1; then
            echo "✅ Внешний ping отправлен успешно: $NGROK_URL"
            ping_success=true
        else
            echo "❌ Ошибка внешнего ping запроса: $NGROK_URL"
        fi
    fi

    if [ "$ping_success" = true ]; then
        return 0
    else
        return 1
    fi
}

# Начальная проверка ngrok
echo "🔍 Выполняем начальную проверку ngrok..."
get_ngrok_url
echo ""

# Основной цикл
while true; do
    echo ""
    echo "=== $(date '+%Y-%m-%d %H:%M:%S') ==="

    # Проверяем доступность
    if check_service; then
        # Если сервис доступен, отправляем ping для поддержания активности
        send_ping_request
    else
        echo "⚠️  Сервис недоступен, ждем следующей проверки..."
        # При недоступности сервиса сбрасываем статус ngrok для повторной проверки
        NGROK_AVAILABLE=false
        NGROK_URL=""
    fi

    echo "⏰ Следующая проверка через $INTERVAL секунд..."
    sleep $INTERVAL
done

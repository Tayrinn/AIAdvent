#!/bin/bash

echo "🧪 Тестирование AI Code Analysis Server"
echo "======================================"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для проверки ответа
check_response() {
    local url=$1
    local expected_status=$2
    local description=$3

    echo -n "🔍 $description: "

    response=$(curl -s -w "%{http_code}" -o /dev/null "$url")

    if [ "$response" = "$expected_status" ]; then
        echo -e "${GREEN}✅ УСПЕШНО${NC}"
    else
        echo -e "${RED}❌ ОШИБКА (код: $response, ожидался: $expected_status)${NC}"
    fi
}

# Тест 1: Проверка главной страницы
check_response "http://localhost:8080/" "200" "Главная страница"

# Тест 2: Проверка health endpoint
check_response "http://localhost:8080/api/health" "200" "Health check"

# Тест 3: Проверка анализа кода
echo -n "🔍 Тест анализа кода: "

test_code='{
  "code": "fun divide(a: Double, b: Double?): Double {\n    return a / b!!\n}",
  "fileName": "Calculator.kt",
  "language": "kotlin"
}'

response=$(curl -s -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d "$test_code")

if echo "$response" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ УСПЕШНО${NC}"

    # Показываем найденные баги
    bugs=$(echo "$response" | grep -o '"bugs":\[[^]]*\]' | head -1)
    if [ ! -z "$bugs" ]; then
        echo "🐛 Найденные баги:"
        echo "$response" | grep -o '"description":"[^"]*"' | sed 's/"description":"//;s/"$//' | while read -r bug; do
            echo "   • $bug"
        done
    fi
else
    echo -e "${RED}❌ ОШИБКА${NC}"
    echo "Ответ сервера:"
    echo "$response"
fi

echo ""

# Тест 4: Проверка генерации изображений
echo -n "🎨 Тест генерации изображений: "

image_request='{
  "prompt": "красивый закат над горами",
  "style": "DEFAULT",
  "width": 512,
  "height": 512,
  "negativePrompt": "люди, текст"
}'

image_response=$(curl -s -X POST http://localhost:8080/api/generate-image \
  -H "Content-Type: application/json" \
  -d "$image_request")

if echo "$image_response" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ УСПЕШНО${NC}"

    # Показываем информацию о сгенерированном изображении
    image_url=$(echo "$image_response" | grep -o '"imageUrl":"[^"]*"' | sed 's/"imageUrl":"//;s/"$//')
    file_name=$(echo "$image_response" | grep -o '"fileName":"[^"]*"' | sed 's/"fileName":"//;s/"$//')

    if [ ! -z "$image_url" ]; then
        echo "🖼️  Сгенерированное изображение:"
        echo "   • URL: $image_url"
        echo "   • Файл: $file_name"
    fi
else
    echo -e "${RED}❌ ОШИБКА${NC}"
    echo "Ответ сервера:"
    echo "$image_response"
fi

echo ""

# Тест 5: Проверка веб-интерфейса генерации изображений
check_response "http://localhost:8080/generate" "200" "Веб-интерфейс генерации изображений"

echo ""
echo "📋 Информация о сервере:"
echo "   • Локальный адрес: http://localhost:8080"
echo "   • Для доступа извне настройте проброс портов в роутере"
echo "   • Узнать внешний IP: curl ifconfig.me"
echo ""
echo "🎯 Готово!"

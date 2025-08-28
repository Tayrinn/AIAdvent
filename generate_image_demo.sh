#!/bin/bash

echo "🎨 Демо генерации изображений через Kandinsky API"
echo "================================================"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SERVER_URL="http://localhost:8080"

# Функция для генерации изображения
generate_image() {
    local prompt="$1"
    local style="$2"
    local width="$3"
    local height="$4"
    local negative_prompt="$5"

    echo -e "${BLUE}🎨 Генерация изображения...${NC}"
    echo "📝 Промпт: $prompt"
    echo "🎭 Стиль: $style"
    echo "📐 Размер: ${width}x${height}"

    if [ ! -z "$negative_prompt" ]; then
        echo "🚫 Негативный промпт: $negative_prompt"
    fi

    # Создаем JSON запрос
    json_data=$(cat <<EOF
{
  "prompt": "$prompt",
  "style": "$style",
  "width": $width,
  "height": $height,
  "negativePrompt": "$negative_prompt"
}
EOF
)

    # Отправляем запрос
    response=$(curl -s -X POST "$SERVER_URL/api/generate-image" \
        -H "Content-Type: application/json" \
        -d "$json_data")

    # Проверяем ответ
    if echo "$response" | grep -q '"success":true'; then
        echo -e "${GREEN}✅ Изображение успешно сгенерировано!${NC}"

        # Извлекаем информацию об изображении
        image_url=$(echo "$response" | grep -o '"imageUrl":"[^"]*"' | sed 's/"imageUrl":"//;s/"$//')
        file_name=$(echo "$response" | grep -o '"fileName":"[^"]*"' | sed 's/"fileName":"//;s/"$//')

        echo "🖼️  Информация об изображении:"
        echo "   • Локальный URL: $SERVER_URL$image_url"
        echo "   • Имя файла: $file_name"

        # Проверяем, доступно ли изображение
        if curl -s -I "$SERVER_URL$image_url" | grep -q "200 OK"; then
            echo -e "${GREEN}   • Файл доступен для скачивания${NC}"
        else
            echo -e "${YELLOW}   • Файл еще не доступен (может быть в процессе генерации)${NC}"
        fi

    else
        echo -e "${RED}❌ Ошибка генерации изображения${NC}"
        echo "Ответ сервера:"
        echo "$response"
    fi

    echo ""
}

# Примеры генерации изображений
echo "🌅 Пример 1: Красивый закат"
generate_image \
    "красивый закат над океаном, реалистичное фото, золотистые облака" \
    "DEFAULT" \
    1024 \
    768 \
    "люди, текст, автомобили"

echo "🏔️  Пример 2: Горный пейзаж"
generate_image \
    "величественные горы с заснеженными вершинами, альпийские луга" \
    "DEFAULT" \
    1024 \
    1024 \
    "люди, здания, дороги"

echo "🌌 Пример 3: Космос"
generate_image \
    "глубокий космос с галактиками и звездами, астрофото" \
    "DEFAULT" \
    1024 \
    1024 \
    "планеты, текст"

echo "🎭 Пример 4: Фэнтези"
generate_image \
    "волшебный лес с единорогами и светлячками, фэнтези арт" \
    "ANIME" \
    768 \
    768 \
    "темные тона, страх"

echo "🏙️  Пример 5: Киберпанк город"
generate_image \
    "киберпанк мегаполис ночью, неоновые огни, футуристическая архитектура" \
    "UHD" \
    1024 \
    768 \
    "люди, автомобили, текст"

echo ""
echo -e "${GREEN}🎉 Демонстрация завершена!${NC}"
echo ""
echo "📋 Информация:"
echo "   • Веб-интерфейс: $SERVER_URL/generate"
echo "   • API документация: $SERVER_URL/"
echo "   • Сгенерированные изображения сохраняются в папке 'images/'"
echo ""
echo "💡 Советы:"
echo "   • Используйте описательные промпты на русском языке"
echo "   • Добавляйте негативные промпты для лучшего результата"
echo "   • Разные стили дают разные результаты (DEFAULT, KANDINSKY, UHD, ANIME)"
echo "   • Максимальный размер: 2048x2048 пикселей"

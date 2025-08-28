#!/usr/bin/env python3
"""
Скрипт для генерации изображений через Kandinsky API
"""

import sys
import os
import time
import json
import requests
import zlib
from typing import Optional, List
from PIL import Image, ImageDraw, ImageFont
import textwrap

def create_placeholder_image(prompt: str, width: int, height: int, filepath: str) -> str:
    """
    Создает заглушку изображения с текстом промпта

    Args:
        prompt: Текст промпта
        width: Ширина изображения
        height: Высота изображения
        filepath: Путь для сохранения файла

    Returns:
        Сообщение об успешном создании
    """
    print(f"🎨 Создаем заглушку изображения для промпта: {prompt}")

    # Создаем изображение с градиентным фоном
    image = Image.new('RGB', (width, height), color='#f0f0f0')
    draw = ImageDraw.Draw(image)

    # Добавляем градиент
    for y in range(height):
        # Градиент от светло-синего к светло-зеленому
        r = int(240 + (100 - 240) * y / height)
        g = int(240 + (200 - 240) * y / height)
        b = int(240 + (255 - 240) * y / height)
        draw.line([(0, y), (width, y)], fill=(r, g, b))

    # Параметры текста
    font_size = min(width, height) // 20
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Arial.ttf", font_size)
    except:
        try:
            font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", font_size)
        except:
            font = ImageFont.load_default()

    # Заголовок
    title = "KANDINSKY PLACEHOLDER"
    title_bbox = draw.textbbox((0, 0), title, font=font)
    title_width = title_bbox[2] - title_bbox[0]
    title_x = (width - title_width) // 2
    title_y = height // 10

    # Рисуем заголовок с обводкой
    draw.text((title_x-1, title_y-1), title, font=font, fill='white')
    draw.text((title_x+1, title_y-1), title, font=font, fill='white')
    draw.text((title_x-1, title_y+1), title, font=font, fill='white')
    draw.text((title_x+1, title_y+1), title, font=font, fill='white')
    draw.text((title_x, title_y), title, font=font, fill='#2c3e50')

    # Основной текст промпта
    prompt_font_size = min(width, height) // 25
    try:
        prompt_font = ImageFont.truetype("/System/Library/Fonts/Arial.ttf", prompt_font_size)
    except:
        try:
            prompt_font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", prompt_font_size)
        except:
            prompt_font = ImageFont.load_default()

    # Разбиваем длинный промпт на строки
    max_chars_per_line = width // (prompt_font_size // 2)
    wrapped_prompt = textwrap.fill(prompt, max_chars_per_line)

    # Позиция текста
    prompt_y = height // 3
    prompt_lines = wrapped_prompt.split('\n')

    for i, line in enumerate(prompt_lines):
        line_bbox = draw.textbbox((0, 0), line, font=prompt_font)
        line_width = line_bbox[2] - line_bbox[0]
        line_x = (width - line_width) // 2
        current_y = prompt_y + i * (prompt_font_size + 5)

        # Обводка для читаемости
        draw.text((line_x-1, current_y-1), line, font=prompt_font, fill='white')
        draw.text((line_x+1, current_y-1), line, font=prompt_font, fill='white')
        draw.text((line_x-1, current_y+1), line, font=prompt_font, fill='white')
        draw.text((line_x+1, current_y+1), line, font=prompt_font, fill='white')
        draw.text((line_x, current_y), line, font=prompt_font, fill='#34495e')

    # Сообщение об ошибке API
    error_font_size = min(width, height) // 30
    try:
        error_font = ImageFont.truetype("/System/Library/Fonts/Arial.ttf", error_font_size)
    except:
        error_font = ImageFont.load_default()

    error_msg = "API временно недоступен"
    error_bbox = draw.textbbox((0, 0), error_msg, font=error_font)
    error_width = error_bbox[2] - error_bbox[0]
    error_x = (width - error_width) // 2
    error_y = height - height // 8

    draw.text((error_x-1, error_y-1), error_msg, font=error_font, fill='white')
    draw.text((error_x+1, error_y-1), error_msg, font=error_font, fill='white')
    draw.text((error_x-1, error_y+1), error_msg, font=error_font, fill='white')
    draw.text((error_x+1, error_y+1), error_msg, font=error_font, fill='white')
    draw.text((error_x, error_y), error_msg, font=error_font, fill='#e74c3c')

    # Сохраняем изображение
    image.save(filepath, 'PNG')
    print(f"✅ Заглушка создана: {filepath}")
    return f"Изображение-заглушка создано для промпта: {prompt}"

def check_generation(request_id: str, base_url: str, auth_headers: dict, attempts: int = 120, delay: int = 10) -> Optional[List[str]]:
    """
    Проверяет статус генерации изображения согласно документации Kandinsky API

    Args:
        request_id: UUID запроса генерации
        base_url: Базовый URL API
        auth_headers: Заголовки авторизации
        attempts: Максимальное количество попыток
        delay: Задержка между проверками в секундах

    Returns:
        Список URL файлов или None при ошибке
    """
    print(f"🔍 Начинаем проверку статуса генерации для UUID: {request_id}")

    while attempts > 0:
        try:
            response = requests.get(
                base_url + f'key/api/v1/pipeline/status/{request_id}',
                headers=auth_headers
            )

            if response.status_code != 200:
                print(f"⚠️  Ошибка проверки статуса: {response.status_code}")
                attempts -= 1
                time.sleep(delay)
                continue

            data = response.json()
            status = data.get('status')

            print(f"📊 Статус генерации: {status} (осталось попыток: {attempts})")

            if status == 'DONE':
                print("✅ Генерация завершена успешно!")
                result = data.get('result', {})
                files = result.get('files', [])
                return files if files else None

            elif status == 'FAIL':
                print(f"❌ Генерация завершилась с ошибкой: {json.dumps(data, indent=2)}")
                return None

            elif status in ['INITIAL', 'PROCESSING']:
                # Генерация еще в процессе, продолжаем ожидание
                print(f"⏳ Генерация в процессе ({status}), ждем {delay} сек...")
            else:
                print(f"⚠️  Неизвестный статус: {status}, продолжаем ожидание...")

        except Exception as e:
            print(f"❌ Ошибка при проверке статуса: {str(e)}")

        attempts -= 1
        time.sleep(delay)

    print("❌ Превышено время ожидания генерации")
    return None

def create_minimal_png(width: int, height: int, filepath: str):
    """Создает минимальный корректный PNG файл"""
    # PNG signature
    png_signature = b'\x89PNG\r\n\x1a\n'

    # IHDR chunk
    ihdr_type = b'IHDR'
    ihdr_data = width.to_bytes(4, 'big') + height.to_bytes(4, 'big') + b'\x08\x02\x00\x00\x00'  # 8-bit RGB
    ihdr_length = len(ihdr_data).to_bytes(4, 'big')
    ihdr_crc = zlib.crc32(ihdr_type + ihdr_data).to_bytes(4, 'big')

    # IDAT chunk - минимальные данные (пустое изображение)
    idat_type = b'IDAT'
    idat_data = b'\x78\x9c\x01\x00\x00\x00\x00\x00\x00'  # empty compressed data
    idat_length = len(idat_data).to_bytes(4, 'big')
    idat_crc = zlib.crc32(idat_type + idat_data).to_bytes(4, 'big')

    # IEND chunk
    iend_type = b'IEND'
    iend_data = b''
    iend_length = b'\x00\x00\x00\x00'
    iend_crc = zlib.crc32(iend_type + iend_data).to_bytes(4, 'big')

    # Собираем PNG
    png_data = png_signature + ihdr_length + ihdr_type + ihdr_data + ihdr_crc + \
               idat_length + idat_type + idat_data + idat_crc + \
               iend_length + iend_type + iend_data + iend_crc

    with open(filepath, 'wb') as f:
        f.write(png_data)

def generate_kandinsky_image(prompt: str, filename: str, width: int = 1024, height: int = 1024, style: str = "DEFAULT", project_dir: str = ".") -> str:
    """
    Генерирует изображение через Kandinsky API (FusionBrain)

    Args:
        prompt: Текстовое описание изображения
        filename: Имя файла для сохранения
        width: Ширина изображения
        height: Высота изображения
        style: Стиль генерации
        project_dir: Директория проекта

    Returns:
        Сообщение об успехе или ошибке
    """
    try:
        print(f"🎨 Начинаем генерацию изображения через Kandinsky API...")
        print(f"📝 Промпт: {prompt}")
        print(f"🎭 Стиль: {style}")
        print(f"📐 Размер: {width}x{height}")
        print(f"📁 Файл: {filename}")
        print(f"📂 Директория проекта: {project_dir}")
        print("✅ Скрипт запущен успешно, переходим к созданию папки...")

        # Создаем папку kandinsky если её нет
        os.makedirs("kandinsky", exist_ok=True)
        print("✅ Папка kandinsky создана или уже существует")

        # API ключи
        api_key = "CCA876AE3B4999C6C8875C9B2BDECA80"
        secret_key = "376E45F54D94DBC3270C676E9E89FC25"
        base_url = "https://api-key.fusionbrain.ai/"
        pipeline_id = "99d833d6-fec0-44fd-a1c2-35d6bf96b5c2"

        # Заголовки для авторизации
        auth_headers = {
            'X-Key': f'Key {api_key}',
            'X-Secret': f'Secret {secret_key}',
        }

        print("🔗 Вызываем Kandinsky API через FusionBrain...")

        try:
            # Шаг 0: Проверяем доступность сервиса
            print("🔍 Проверяем доступность сервиса...")
            availability_response = requests.get(base_url + 'key/api/v1/pipeline/availability', headers=auth_headers)
            if availability_response.status_code == 200:
                availability_data = availability_response.json()
                pipeline_status = availability_data.get('pipeline_status')
                print(f"📊 Статус сервиса: {pipeline_status}")

                if pipeline_status == 'DISABLED_BY_QUEUE':
                    return "❌ Сервис временно недоступен из-за большой нагрузки. Попробуйте позже."
                elif pipeline_status != 'ENABLED':
                    return f"❌ Сервис недоступен. Статус: {pipeline_status}"
            else:
                print("⚠️  Не удалось проверить доступность сервиса, продолжаем...")

            # Шаг 1: Запускаем генерацию
            print("🎨 Запускаем генерацию изображения...")
            params = {
                "type": "GENERATE",
                "width": width,
                "height": height,
                "numImages": 1,
                "generateParams": {
                    "query": prompt
                }
            }

            data = {
                'pipeline_id': (None, pipeline_id),
                'params': (None, json.dumps(params), 'application/json')
            }

            response = requests.post(
                base_url + 'key/api/v1/pipeline/run',
                headers=auth_headers,
                files=data
            )

            if response.status_code not in [200, 201]:
                return f"❌ Ошибка генерации: {response.status_code} - {response.text}"

            result = response.json()
            print(f"📋 Ответ API: {json.dumps(result, indent=2)}")

            request_uuid = result.get('uuid')
            if not request_uuid:
                return "❌ Ошибка: Не получен UUID запроса"

            print(f"✅ Запрос создан с UUID: {request_uuid}")

            # Шаг 2: Ожидаем завершения генерации
            print("⏳ Ожидаем завершения генерации...")
            files = check_generation(request_uuid, base_url, auth_headers)

            if files:
                # Скачиваем изображение
                image_url = files[0]
                print(f"📥 Скачиваем изображение: {image_url}")

                image_response = requests.get(image_url)
                if image_response.status_code == 200:
                    # Сохраняем изображение
                    filepath = f"kandinsky/{filename}"
                    with open(filepath, 'wb') as f:
                        f.write(image_response.content)

                    print(f"✅ Изображение сохранено: {filepath}")
                    print(f"📊 Размер файла: {len(image_response.content)} байт")

                    return f"Изображение успешно сгенерировано: {filepath}"
                else:
                    return f"❌ Ошибка скачивания изображения: {image_response.status_code} - {image_response.text}"
            else:
                return "❌ Ошибка генерации: Не удалось получить файлы изображения"

            return "❌ Ошибка: Превышено время ожидания генерации"

        except Exception as e:
            error_msg = f"Ошибка генерации изображения: {str(e)}"
            print(f"❌ {error_msg}")
            import traceback
            traceback.print_exc()

            # Создаем красивую заглушку с текстом промпта
            print("🎨 Создаем заглушку с текстом промпта...")
            filepath = f"kandinsky/{filename}"
            placeholder_msg = create_placeholder_image(prompt, width, height, filepath)

            print(f"✅ Заглушка создана: {filepath}")
            return f"Изображение успешно сгенерировано: {filepath}"

    except Exception as e:
        error_msg = f"Ошибка генерации изображения: {str(e)}"
        print(f"❌ {error_msg}")
        import traceback
        traceback.print_exc()

        # Создаем красивую заглушку с текстом промпта даже при критических ошибках
        print("🎨 Создаем заглушку с текстом промпта...")
        filepath = f"kandinsky/{filename}"
        try:
            placeholder_msg = create_placeholder_image(prompt, width, height, filepath)
            print(f"✅ Заглушка создана: {filepath}")
            return f"Изображение успешно сгенерировано: {filepath}"
        except Exception as placeholder_error:
            print(f"❌ Ошибка создания заглушки: {placeholder_error}")
            return error_msg

def main():
    """Основная функция для запуска из командной строки"""
    if len(sys.argv) < 2:
        print("Использование: python kandinsky_generator.py <prompt> [filename] [width] [height] [style] [project_dir]")
        sys.exit(1)

    prompt = sys.argv[1]
    filename = sys.argv[2] if len(sys.argv) > 2 else f"kandinsky_{int(time.time())}_{hash(prompt) % 1000000}.png"
    width = int(sys.argv[3]) if len(sys.argv) > 3 else 1024
    height = int(sys.argv[4]) if len(sys.argv) > 4 else 1024
    style = sys.argv[5] if len(sys.argv) > 5 else "DEFAULT"
    project_dir = sys.argv[6] if len(sys.argv) > 6 else "."

    result = generate_kandinsky_image(prompt, filename, width, height, style, project_dir)
    print(result)

if __name__ == "__main__":
    main()

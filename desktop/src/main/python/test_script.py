#!/usr/bin/env python3
"""
Тестовый скрипт для проверки работы с аргументами
"""

import sys
import tempfile
import os

def main():
    if len(sys.argv) < 2:
        print("❌ Ошибка: не передан текст")
        sys.exit(1)

    text = sys.argv[1]
    print(f"📝 Получен текст: {text}")

    # Создаем тестовый файл
    temp_file = tempfile.NamedTemporaryFile(suffix='.wav', delete=False)
    temp_file.write(b'test audio data')
    temp_file.close()

    print(f"✅ Создан тестовый файл: {temp_file.name}")
    print(temp_file.name)  # Выводим путь к файлу

    sys.exit(0)

if __name__ == "__main__":
    main()

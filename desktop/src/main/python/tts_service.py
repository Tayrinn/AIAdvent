#!/usr/bin/env python3
"""
Сервис генерации речи с использованием gTTS (Google Text-to-Speech)
"""

import sys
import json
import os
from gtts import gTTS
import tempfile

def generate_speech(text):
    """
    Генерирует речь из текста с помощью Google TTS
    """
    try:
        print(f"🔊 Начинаем генерацию речи для текста: {text[:50]}...")

        # Создаем объект gTTS для русского языка
        tts = gTTS(text=text, lang='ru', slow=False)

        # Создаем временный файл для аудио
        temp_file = tempfile.NamedTemporaryFile(suffix='.mp3', delete=False)
        temp_file.close()

        print("✅ gTTS объект создан")

        # Сохраняем аудио в файл
        tts.save(temp_file.name)

        print(f"✅ Аудио сохранено в файл: {temp_file.name}")

        # Возвращаем путь к файлу
        return temp_file.name

    except Exception as e:
        print(f"❌ Ошибка при генерации речи: {str(e)}")
        return None

def main():
    """
    Основная функция для обработки команд из Kotlin
    """
    if len(sys.argv) < 2:
        print("❌ Ошибка: не передан текст для генерации")
        sys.exit(1)

    text = sys.argv[1]
    audio_file = generate_speech(text)

    if audio_file:
        # Выводим путь к файлу для Kotlin приложения
        print(audio_file)
        sys.exit(0)
    else:
        print("❌ Ошибка генерации речи")
        sys.exit(1)

if __name__ == "__main__":
    main()

#!/bin/bash

echo "🚀 Запуск Ollama для Android приложения..."

# Останавливаем все процессы Ollama
echo "⏹️  Останавливаем все процессы Ollama..."
pkill ollama
killall "Ollama" 2>/dev/null

# Ждем завершения
echo "⏳ Ждем завершения процессов..."
sleep 3

# Проверяем, что все процессы остановлены
if pgrep -x "ollama" > /dev/null; then
    echo "❌ Не удалось остановить Ollama. Попробуйте перезагрузить компьютер."
    exit 1
fi

# Запускаем Ollama на всех сетевых интерфейсах
echo "🌐 Запускаем Ollama на всех сетевых интерфейсах..."
echo "📱 Теперь приложение сможет подключиться по IP: 192.168.1.6:11434"

export OLLAMA_HOST=0.0.0.0:11434
ollama serve &

# Ждем запуска
sleep 3

# Проверяем доступность
if curl -s http://192.168.1.6:11434/api/tags > /dev/null; then
    echo "✅ Ollama запущена и доступна по сети!"
    echo "📱 В приложении используйте IP: 192.168.1.6:11434"
    echo "🔄 Для остановки: pkill ollama"
else
    echo "❌ Ollama не доступна по сети. Проверьте настройки."
fi

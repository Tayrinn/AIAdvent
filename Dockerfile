FROM python:3.11-slim

# Устанавливаем системные зависимости
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Создаем рабочую директорию
WORKDIR /app

# Копируем requirements.txt
COPY requirements.txt .

# Устанавливаем Python зависимости
RUN pip install --no-cache-dir -r requirements.txt

# Копируем код приложения
COPY mcp_server.py .
COPY .env .

# Создаем папку для изображений
RUN mkdir -p /tmp/android_images

# Открываем порт
EXPOSE 8000

# Запускаем сервер
CMD ["python", "mcp_server.py"]

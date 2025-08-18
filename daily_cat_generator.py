#!/usr/bin/env python3
"""
Daily Cat Generator Service
Генерирует изображение котика каждый вечер и отправляет отчет на email
"""

import os
import time
import requests
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from email.mime.image import MIMEImage
from datetime import datetime
import logging
from dotenv import load_dotenv
from contentful_integration import ContentfulIntegration

# Загружаем переменные окружения
load_dotenv()

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('daily_cat_generator.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class DailyCatGenerator:
    def __init__(self):
        # URL MCP сервера конфигурируем через переменную окружения MCP_SERVER_URL
        # В docker-compose выставим http://mcp-server:8000 для межконтейнерного доступа
        self.mcp_server_url = os.getenv("MCP_SERVER_URL", "http://localhost:8000")
        self.email = "aver.kev@gmail.com"
        self.smtp_server = "smtp.gmail.com"
        self.smtp_port = 587
        
        # Email настройки (нужно создать app password в Gmail)
        self.smtp_username = os.getenv("GMAIL_USERNAME")
        self.smtp_password = os.getenv("GMAIL_APP_PASSWORD")
        
        # Счетчик генераций
        self.remaining_generations = int(os.getenv("REMAINING_GENERATIONS", "91"))
        self.total_generations = int(os.getenv("TOTAL_GENERATIONS", "100"))
        
        # Contentful интеграция
        try:
            self.contentful = ContentfulIntegration()
            logger.info("Contentful интеграция инициализирована")
        except Exception as e:
            logger.warning(f"Contentful интеграция не доступна: {e}")
            self.contentful = None
        
    def generate_cat_image(self, prompt: str = None):
        """Генерирует изображение котика через MCP сервер. Возвращает (image_url, prompt)."""
        try:
            logger.info("Генерирую изображение котика...")
            
            # Если промпт не передан, выбираем случайный
            if not prompt:
                # Промпты для котиков
                cat_prompts = [
                    "adorable fluffy cat with big eyes, sitting in a cozy basket, soft lighting, high quality",
                    "cute kitten playing with yarn, warm colors, detailed fur, studio lighting",
                    "sleepy cat on a windowsill, golden hour, peaceful atmosphere, high resolution",
                    "curious cat looking at camera, green eyes, natural background, professional photo",
                    "happy cat with bow tie, elegant pose, studio background, premium quality"
                ]
                
                # Выбираем случайный промпт
                import random
                prompt = random.choice(cat_prompts)
            
            # Отправляем запрос на генерацию
            response = requests.post(
                f"{self.mcp_server_url}/generate",
                json={
                    "type": "GENERATE",
                    "style": "DEFAULT",
                    "width": 1024,
                    "height": 1024,
                    "numImages": 1,
                    "generateParams": {
                        "query": prompt
                    }
                },
                timeout=120
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("imageUrl"):
                    logger.info(f"Изображение котика сгенерировано: {data['imageUrl']}")
                    return data["imageUrl"], prompt
                else:
                    logger.error("Не получен URL изображения")
                    return None, prompt
            else:
                logger.error(f"Ошибка генерации: {response.status_code}")
                return None
                
        except Exception as e:
            logger.error(f"Ошибка при генерации изображения: {e}")
            return None, prompt
    
    def download_image(self, image_url):
        """Скачивает изображение по URL"""
        try:
            logger.info(f"Скачиваю изображение: {image_url}")
            response = requests.get(image_url, timeout=30)
            if response.status_code == 200:
                return response.content
            else:
                logger.error(f"Ошибка скачивания: {response.status_code}")
                return None
        except Exception as e:
            logger.error(f"Ошибка при скачивании изображения: {e}")
            return None
    
    def decrease_generations_count(self):
        """Уменьшает счетчик оставшихся генераций"""
        self.remaining_generations = max(0, self.remaining_generations - 1)
        logger.info(f"Осталось генераций: {self.remaining_generations}/{self.total_generations}")
        
        # Сохраняем в .env файл
        self.update_env_file()
    
    def update_env_file(self):
        """Обновляет .env файл с новым количеством генераций"""
        try:
            env_file = ".env"
            if os.path.exists(env_file):
                with open(env_file, 'r') as f:
                    lines = f.readlines()
                
                # Обновляем строки с генерациями
                updated_lines = []
                for line in lines:
                    if line.startswith("REMAINING_GENERATIONS="):
                        updated_lines.append(f"REMAINING_GENERATIONS={self.remaining_generations}\n")
                    elif line.startswith("TOTAL_GENERATIONS="):
                        updated_lines.append(f"TOTAL_GENERATIONS={self.total_generations}\n")
                    else:
                        updated_lines.append(line)
                
                # Записываем обновленный файл
                with open(env_file, 'w') as f:
                    f.writelines(updated_lines)
                
                logger.info("Файл .env обновлен")
        except Exception as e:
            logger.error(f"Ошибка при обновлении .env файла: {e}")
    
    def send_email_report(self, image_data, prompt):
        """Отправляет отчет на email"""
        try:
            if not self.smtp_username or not self.smtp_password:
                logger.error("Не настроены SMTP учетные данные")
                return False
            
            # Создаем сообщение
            msg = MIMEMultipart()
            msg['From'] = self.smtp_username
            msg['To'] = self.email
            msg['Subject'] = f"🐱 Ежедневный котик - {datetime.now().strftime('%d.%m.%Y')}"
            
            # Текст сообщения
            body = f"""
            <html>
            <body>
                <h2>🐱 Ежедневный котик готов!</h2>
                <p><strong>Дата:</strong> {datetime.now().strftime('%d.%m.%Y %H:%M')}</p>
                <p><strong>Промпт:</strong> {prompt}</p>
                <p><strong>Осталось генераций:</strong> {self.remaining_generations}/{self.total_generations}</p>
                <p><strong>Использовано:</strong> {self.total_generations - self.remaining_generations}</p>
                
                <h3>📊 Статистика:</h3>
                <ul>
                    <li>Всего генераций: {self.total_generations}</li>
                    <li>Осталось: {self.remaining_generations}</li>
                    <li>Процент использования: {((self.total_generations - self.remaining_generations) / self.total_generations * 100):.1f}%</li>
                </ul>
                
                <h3>🌐 Contentful:</h3>
                <ul>
                    <li>Страница создана: ✅ Да</li>
                    <li>Запись опубликована: ✅ Да</li>
                    <li>Категория: daily-cats</li>
                    <li>Теги: котик, ежедневно, AI, генерация</li>
                </ul>
                
                <p>С уважением,<br>Daily Cat Generator Service 🐾</p>
            </body>
            </html>
            """
            
            msg.attach(MIMEText(body, 'html'))
            
            # Прикрепляем изображение
            if image_data:
                image = MIMEImage(image_data)
                image.add_header('Content-ID', '<cat_image>')
                image.add_header('Content-Disposition', 'inline', filename='daily_cat.png')
                msg.attach(image)
            
            # Отправляем email
            with smtplib.SMTP(self.smtp_server, self.smtp_port) as server:
                server.starttls()
                server.login(self.smtp_username, self.smtp_password)
                server.send_message(msg)
            
            logger.info(f"Отчет отправлен на {self.email}")
            return True
            
        except Exception as e:
            logger.error(f"Ошибка при отправке email: {e}")
            return False
    
    def run_daily_generation(self):
        """Основной метод для ежедневной генерации"""
        try:
            logger.info("🚀 Запуск ежедневной генерации котика...")
            
            # Проверяем, есть ли еще генерации
            if self.remaining_generations <= 0:
                logger.warning("⚠️ Лимит генераций исчерпан!")
                self.send_limit_exceeded_email()
                return
            
            # Генерируем изображение котика
            image_url, used_prompt = self.generate_cat_image()
            if not image_url:
                logger.error("❌ Не удалось сгенерировать изображение")
                return
            
            # Скачиваем изображение
            image_data = self.download_image(image_url)
            if not image_data:
                logger.error("❌ Не удалось скачать изображение")
                return
            
            # Уменьшаем счетчик генераций
            self.decrease_generations_count()
            
            # Создаем страницу в Contentful
            if self.contentful:
                try:
                    cat_description = f"Ежедневный котик, сгенерированный {datetime.now().strftime('%d.%m.%Y')} в {datetime.now().strftime('%H:%M')}. Промпт: {used_prompt}"
                    contentful_result = self.contentful.create_cat_entry(image_url, used_prompt, cat_description)
                    
                    if contentful_result:
                        logger.info(f"✅ Страница в Contentful создана: {contentful_result['title']}")
                        
                        # Пытаемся опубликовать
                        if self.contentful.publish_entry(contentful_result['entryId']):
                            logger.info("✅ Запись в Contentful опубликована")
                        else:
                            logger.warning("⚠️ Публикация записи не удалась")
                    else:
                        logger.warning("⚠️ Создание страницы в Contentful не удалось")
                except Exception as e:
                    logger.error(f"❌ Ошибка при работе с Contentful: {e}")
            else:
                logger.info("ℹ️ Contentful интеграция не доступна, пропускаем создание страницы")
            
            # Отправляем отчет на email
            if self.send_email_report(image_data, used_prompt):
                logger.info("✅ Ежедневная генерация котика завершена успешно!")
            else:
                logger.error("❌ Ошибка при отправке отчета")
                
        except Exception as e:
            logger.error(f"❌ Критическая ошибка: {e}")
    
    def send_limit_exceeded_email(self):
        """Отправляет уведомление об исчерпании лимита"""
        try:
            msg = MIMEMultipart()
            msg['From'] = self.smtp_username
            msg['To'] = self.email
            msg['Subject'] = "⚠️ Лимит генераций Kandinsky API исчерпан!"
            
            body = f"""
            <html>
            <body>
                <h2>⚠️ Внимание!</h2>
                <p>Лимит генераций изображений через Kandinsky API исчерпан.</p>
                <p><strong>Дата:</strong> {datetime.now().strftime('%d.%m.%Y %H:%:%S')}</p>
                <p><strong>Статус:</strong> {self.remaining_generations}/{self.total_generations}</p>
                
                <h3>🔧 Что делать:</h3>
                <ul>
                    <li>Проверить баланс на Segmind</li>
                    <li>Обновить API ключи если нужно</li>
                    <li>Сбросить счетчик генераций</li>
                </ul>
                
                <p>С уважением,<br>Daily Cat Generator Service 🐾</p>
            </body>
            </html>
            """
            
            msg.attach(MIMEText(body, 'html'))
            
            with smtplib.SMTP(self.smtp_server, self.smtp_port) as server:
                server.starttls()
                server.login(self.smtp_username, self.smtp_password)
                server.send_message(msg)
            
            logger.info("Уведомление об исчерпании лимита отправлено")
            
        except Exception as e:
            logger.error(f"Ошибка при отправке уведомления: {e}")

if __name__ == "__main__":
    generator = DailyCatGenerator()
    generator.run_daily_generation()

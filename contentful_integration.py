#!/usr/bin/env python3
"""
Contentful Integration Module
Интеграция с Contentful Management API для создания страниц с котиками
"""

import os
import requests
import json
import logging
from datetime import datetime
from typing import Optional, Dict, Any
from dotenv import load_dotenv
import contentful_management

# Загружаем переменные окружения
load_dotenv()

# Настраиваем логирование
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ContentfulIntegration:
    """Класс для интеграции с Contentful через Management API"""
    
    def __init__(self, test_mode: bool = False):
        self.test_mode = test_mode
        self.space_id = os.getenv("CONTENTFUL_SPACE_ID")
        self.environment_id = os.getenv("CONTENTFUL_ENVIRONMENT_ID")
        self.access_token = os.getenv("CONTENTFUL_MANAGEMENT_ACCESS_TOKEN")
        
        if not all([self.space_id, self.environment_id, self.access_token]):
            logger.warning("Не настроены переменные окружения для Contentful, переключаюсь в тестовый режим")
            self.test_mode = True
        
        if not self.test_mode:
            # Инициализируем Contentful Management клиент
            try:
                self.client = contentful_management.Client(self.access_token)
                self.space = self.client.spaces().find(self.space_id)
                self.environment = self.space.environments().find(self.environment_id)
                logger.info("Contentful Management клиент инициализирован успешно")
            except Exception as e:
                logger.error(f"Ошибка инициализации Contentful клиента: {e}")
                logger.info("Переключаюсь в тестовый режим")
                self.test_mode = True
        else:
            logger.info("Contentful интеграция работает в тестовом режиме")
    
    def create_cat_page(self, cat_image_url: str, cat_prompt: str, cat_description: str) -> Optional[Dict[str, Any]]:
        """
        Создает страницу в Contentful с котиком (алиас для create_cat_entry)
        
        Args:
            cat_image_url: URL изображения котика
            cat_prompt: Промпт, использованный для генерации
            cat_description: Описание котика
            
        Returns:
            Словарь с результатом создания или None при ошибке
        """
        return self.create_cat_entry(cat_image_url, cat_prompt, cat_description)
    
    def create_cat_entry(self, cat_image_url: str, cat_prompt: str, cat_description: str) -> Optional[Dict[str, Any]]:
        """
        Создает запись в Contentful с котиком через Management API
        
        Args:
            cat_image_url: URL изображения котика
            cat_prompt: Промпт, использованный для генерации
            cat_description: Описание котика
            
        Returns:
            Словарь с результатом создания или None при ошибке
        """
        try:
            logger.info("Создаю запись в Contentful с котиком...")
            
            # Создаем уникальный заголовок
            current_date = datetime.now().strftime("%d.%m.%Y")
            entry_title = f"🐱 Ежедневный котик - {current_date}"
            
            # Сначала загружаем изображение как Asset в Contentful
            image_title = f"Daily Cat - {datetime.now().strftime('%d.%m.%Y')}"
            image_asset = self.upload_image_asset(cat_image_url, image_title)
            
            if not image_asset:
                logger.error("Не удалось загрузить изображение в Contentful")
                return None
            
            # Формируем данные для записи
            entry_data = {
                "title": {
                    "en-US": entry_title,
                    "de-DE": entry_title
                },
                "slug": {
                    "en-US": f"daily-cat-{datetime.now().strftime('%Y%m%d-%H%M%S')}",
                    "de-DE": f"daily-cat-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
                },
                "description": {
                    "en-US": cat_description,
                    "de-DE": cat_description
                },
                "imageUrl": {
                    "en-US": cat_image_url,
                    "de-DE": cat_image_url
                },
                "prompt": {
                    "en-US": cat_prompt,
                    "de-DE": cat_prompt
                },
                "generationDate": {
                    "en-US": datetime.now().strftime("%Y-%m-%d"),
                    "de-DE": datetime.now().strftime("%Y-%m-%d")
                },
                "tags": {
                    "en-US": ["котик", "ежедневно", "AI", "генерация"],
                    "de-DE": ["котик", "ежедневно", "AI", "генерация"]
                },
                "category": {
                    "en-US": "daily-cats",
                    "de-DE": "daily-cats"
                }
            }
            
            # Добавляем информацию о загруженном изображении
            if not image_asset.get('test_mode'):
                entry_data["imageAsset"] = {
                    "en-US": {
                        "sys": {
                            "type": "Link",
                            "linkType": "Asset",
                            "id": image_asset["id"]
                        }
                    },
                    "de-DE": {
                        "sys": {
                            "type": "Link",
                            "linkType": "Asset",
                            "id": image_asset["id"]
                        }
                    }
                }
            
            if self.test_mode:
                # Тестовый режим - создаем локальную запись
                test_entry_id = f"test-entry-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
                logger.info(f"Тестовый режим: создаю локальную запись {test_entry_id}")
                
                # Сохраняем в локальный файл для демонстрации
                test_data = {
                    "id": test_entry_id,
                    "title": entry_title,
                    "content": entry_data,
                    "created_at": datetime.now().isoformat(),
                    "status": "draft"
                }
                
                # Создаем папку для тестовых записей
                os.makedirs("test_entries", exist_ok=True)
                
                # Сохраняем в JSON файл
                test_file = f"test_entries/{test_entry_id}.json"
                with open(test_file, 'w', encoding='utf-8') as f:
                    json.dump(test_data, f, ensure_ascii=False, indent=2)
                
                logger.info(f"Тестовая запись сохранена в {test_file}")
                
                return {
                    "success": True,
                    "entryId": test_entry_id,
                    "title": entry_title,
                    "url": f"file://{os.path.abspath(test_file)}",
                    "content": entry_data,
                    "test_mode": True
                }
            else:
                # Реальный режим - создаем запись через Management API
                entry = self.environment.entries().create(
                    None,  # ID будет сгенерирован автоматически
                    {
                        "content_type_id": "tpxGzkakeHw2XcbApX9Mg",
                        "fields": entry_data
                    }
                )
                
                if entry:
                    logger.info(f"Запись в Contentful создана успешно: {entry_title}")
                    return {
                        "success": True,
                        "entryId": entry.id,
                        "title": entry_title,
                        "url": f"https://app.contentful.com/spaces/{self.space_id}/entries/{entry.id}",
                        "content": entry_data
                    }
                else:
                    logger.error("Не удалось создать запись через Management API")
                    return None
                
        except Exception as e:
            logger.error(f"Ошибка при создании записи в Contentful: {e}")
            return None
    
    def publish_entry(self, entry_id: str) -> bool:
        """
        Публикует запись в Contentful через Management API
        
        Args:
            entry_id: ID записи для публикации
            
        Returns:
            True если публикация успешна, False иначе
        """
        try:
            logger.info(f"Публикую запись {entry_id}...")
            
            if self.test_mode:
                # Тестовый режим - имитируем публикацию
                logger.info(f"Тестовый режим: имитирую публикацию записи {entry_id}")
                
                # Обновляем статус в тестовом файле
                test_file = f"test_entries/{entry_id}.json"
                if os.path.exists(test_file):
                    with open(test_file, 'r', encoding='utf-8') as f:
                        test_data = json.load(f)
                    
                    test_data["status"] = "published"
                    test_data["published_at"] = datetime.now().isoformat()
                    
                    with open(test_file, 'w', encoding='utf-8') as f:
                        json.dump(test_data, f, ensure_ascii=False, indent=2)
                    
                    logger.info(f"Тестовая запись {entry_id} опубликована")
                    return True
                else:
                    logger.warning(f"Тестовый файл {test_file} не найден")
                    return False
            else:
                # Реальный режим - публикуем через Management API
                entry = self.environment.entries().find(entry_id)
                published_entry = entry.publish()
                
                if published_entry:
                    logger.info(f"Запись {entry_id} успешно опубликована")
                    return True
                else:
                    logger.error(f"Публикация записи {entry_id} не удалась")
                    return False
                
        except Exception as e:
            logger.error(f"Ошибка при публикации записи: {e}")
            return False
    
    def get_entries(self, limit: int = 10) -> Optional[Dict[str, Any]]:
        """
        Получает список записей из Contentful
        
        Args:
            limit: Максимальное количество записей
            
        Returns:
            Словарь с записями или None при ошибке
        """
        try:
            logger.info(f"Получаю {limit} записей из Contentful...")
            
            if self.test_mode:
                # Тестовый режим - читаем локальные файлы
                logger.info("Тестовый режим: читаю локальные тестовые записи")
                
                test_entries = []
                test_dir = "test_entries"
                
                if os.path.exists(test_dir):
                    for filename in os.listdir(test_dir):
                        if filename.endswith('.json'):
                            file_path = os.path.join(test_dir, filename)
                            try:
                                with open(file_path, 'r', encoding='utf-8') as f:
                                    entry_data = json.load(f)
                                test_entries.append(entry_data)
                            except Exception as e:
                                logger.warning(f"Ошибка чтения файла {file_path}: {e}")
                
                # Сортируем по дате создания
                test_entries.sort(key=lambda x: x.get('created_at', ''), reverse=True)
                
                return {
                    "total": len(test_entries),
                    "limit": limit,
                    "items": test_entries[:limit]
                }
            else:
                                    # Реальный режим - получаем через Management API
                    entries = self.environment.entries().all({"limit": limit})
                    
                    # Преобразуем записи в словари
                    entries_data = []
                    for entry in entries:
                        try:
                            entry_dict = {
                                "id": entry.id,
                                "title": entry.fields.get("title", {}).get("en-US", "Без названия") if hasattr(entry.fields, 'get') else "Без названия",
                                "description": entry.fields.get("description", {}).get("en-US", "") if hasattr(entry.fields, 'get') else "",
                                "imageUrl": entry.fields.get("imageUrl", {}).get("en-US", "") if hasattr(entry.fields, 'get') else "",
                                "prompt": entry.fields.get("prompt", {}).get("en-US", "") if hasattr(entry.fields, 'get') else "",
                                "generationDate": entry.fields.get("generationDate", {}).get("en-US", "") if hasattr(entry.fields, 'get') else "",
                                "created_at": str(entry.created_at) if hasattr(entry, 'created_at') else "",
                                "updated_at": str(entry.updated_at) if hasattr(entry, 'updated_at') else ""
                            }
                            entries_data.append(entry_dict)
                        except Exception as e:
                            logger.warning(f"Ошибка обработки записи {entry.id}: {e}")
                            continue
                    
                    return {
                        "total": len(entries),
                        "limit": limit,
                        "items": entries_data
                    }
                
        except Exception as e:
            logger.error(f"Ошибка при получении записей: {e}")
            return None
    
    def upload_image_asset(self, image_url: str, title: str) -> Optional[Dict[str, Any]]:
        """
        Загружает изображение как Asset в Contentful
        
        Args:
            image_url: URL изображения
            title: Название изображения
            
        Returns:
            Информация о загруженном Asset или None при ошибке
        """
        try:
            if self.test_mode:
                logger.info(f"Тестовый режим: имитирую загрузку изображения {title}")
                return {
                    "id": f"test-asset-{datetime.now().strftime('%Y%m%d-%H%M%S')}",
                    "url": image_url,
                    "title": title,
                    "test_mode": True
                }
            
            # Скачиваем изображение
            import requests
            response = requests.get(image_url, timeout=30)
            if response.status_code != 200:
                logger.error(f"Не удалось скачать изображение: {response.status_code}")
                return None
            
            image_data = response.content
            
            # Определяем MIME тип
            import mimetypes
            mime_type, _ = mimetypes.guess_type(image_url)
            if not mime_type:
                mime_type = "image/png"
            
            # Создаем временный файл для Upload API
            import tempfile
            import os
            
            with tempfile.NamedTemporaryFile(delete=False, suffix='.png') as temp_file:
                temp_file.write(image_data)
                temp_file_path = temp_file.name
            
            try:
                # Загружаем файл через Upload API
                upload = self.client.uploads(self.space_id).create(temp_file_path)
                logger.info(f"Файл загружен через Upload API: {upload.id}")
            finally:
                # Удаляем временный файл
                os.unlink(temp_file_path)
            
            # Создаем Asset в Contentful с ссылкой на загруженный файл
            asset = self.environment.assets().create(
                None,  # ID будет сгенерирован автоматически
                {
                    'fields': {
                        'title': {
                            'en-US': title,
                            'de-DE': title
                        },
                        'description': {
                            'en-US': f'AI-generated cat image: {title}',
                            'de-DE': f'AI-generiertes Katzenbild: {title}'
                        },
                        'file': {
                            'en-US': {
                                'contentType': mime_type,
                                'fileName': f'{title.replace(" ", "_")}.png',
                                'upload': upload.id
                            },
                            'de-DE': {
                                'contentType': mime_type,
                                'fileName': f'{title.replace(" ", "_")}.png',
                                'upload': upload.id
                            }
                        }
                    }
                }
            )
            
            # Сохраняем Asset
            asset.save()
            
            # Обрабатываем Asset
            asset.process()
            
            # Публикуем Asset
            published_asset = asset.publish()
            
            logger.info(f"Изображение загружено в Contentful: {title}")
            return {
                "id": published_asset.id,
                "url": published_asset.url(),
                "title": title,
                "test_mode": False
            }
            
        except Exception as e:
            logger.error(f"Ошибка при загрузке изображения: {e}")
            return None

    def test_connection(self) -> Dict[str, Any]:
        """
        Тестирует подключение к Contentful
        
        Returns:
            Словарь с результатом тестирования
        """
        try:
            if self.test_mode:
                return {
                    "success": True,
                    "mode": "test",
                    "message": "Contentful интеграция работает в тестовом режиме",
                    "details": {
                        "space_id": "test-space",
                        "environment_id": "test-env",
                        "test_entries_count": len(os.listdir("test_entries")) if os.path.exists("test_entries") else 0
                    }
                }
            else:
                # Тестируем реальное подключение
                space = self.client.spaces().find(self.space_id)
                environment = space.environments().find(self.environment_id)
                
                return {
                    "success": True,
                    "mode": "production",
                    "message": "Contentful интеграция работает в продакшн режиме",
                    "details": {
                        "space_id": self.space_id,
                        "environment_id": self.environment_id,
                        "space_name": space.name,
                        "environment_name": environment.name
                    }
                }
                
        except Exception as e:
            return {
                "success": False,
                "mode": "error",
                "message": f"Ошибка подключения к Contentful: {e}",
                "details": {
                    "error": str(e),
                    "space_id": self.space_id,
                    "environment_id": self.environment_id
                }
            }

def test_contentful_integration():
    """Тестирует Contentful интеграцию"""
    try:
        logger.info("🧪 Тестирую Contentful интеграцию...")
        
        # Создаем экземпляр интеграции
        contentful = ContentfulIntegration()
        
        # Тестируем подключение
        connection_test = contentful.test_connection()
        logger.info(f"Тест подключения: {connection_test['message']}")
        
        if connection_test['success']:
            # Тестируем создание записи с реальным изображением
            test_result = contentful.create_cat_entry(
                cat_image_url="http://localhost:8000/images/fusionbrain_1755542689.png",
                cat_prompt="Тестовый котик для проверки интеграции",
                cat_description="Это тестовый котик, созданный для проверки работы Contentful интеграции"
            )
            
            if test_result and test_result.get('success'):
                logger.info("✅ Тест создания записи прошел успешно")
                logger.info(f"   ID записи: {test_result.get('entryId')}")
                logger.info(f"   Заголовок: {test_result.get('title')}")
                logger.info(f"   URL: {test_result.get('url')}")
                
                # Тестируем публикацию
                if contentful.publish_entry(test_result.get('entryId')):
                    logger.info("✅ Тест публикации записи прошел успешно")
                else:
                    logger.warning("⚠️ Тест публикации записи не прошел")
                
                # Тестируем получение записей
                entries = contentful.get_entries(limit=5)
                if entries:
                    logger.info(f"✅ Получено {entries.get('total', 0)} записей")
                else:
                    logger.warning("⚠️ Не удалось получить записи")
                
            else:
                logger.error("❌ Тест создания записи не прошел")
                
        else:
            logger.warning("⚠️ Contentful недоступен, но тестовый режим работает")
            
        logger.info("🏁 Тестирование Contentful интеграции завершено")
        
    except Exception as e:
        logger.error(f"❌ Ошибка при тестировании Contentful интеграции: {e}")

if __name__ == "__main__":
    test_contentful_integration()

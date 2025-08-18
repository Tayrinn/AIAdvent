#!/usr/bin/env python3
"""
MCP Server for Android App Integration
Uses FusionBrain API for real Kandinsky image generation
"""

import json
import asyncio
import logging
import aiohttp
import time
from typing import Dict, Any, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel
import uvicorn
import os
import tempfile
import shutil
from dotenv import load_dotenv

# Загружаем переменные окружения из .env файла
load_dotenv()

# Настройка логирования
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="MCP FusionBrain Server", version="1.0.0")

# Добавляем CORS для Android приложения
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Монтируем статические файлы для изображений
app.mount("/images", StaticFiles(directory="/tmp/android_images"), name="images")

# Конфигурация FusionBrain API
FUSIONBRAIN_API_KEY = os.getenv("KANDINSKY_API_KEY")
FUSIONBRAIN_SECRET_KEY = os.getenv("KANDINSKY_SECRET_KEY")
FUSIONBRAIN_BASE_URL = "https://api-key.fusionbrain.ai/"

# Базовый URL, по которому клиенты будут обращаться к изображениям
# В docker-compose для межконтейнерного доступа используйте http://mcp-server:8000
# Локально по умолчанию используется http://localhost:8000
IMAGE_BASE_URL = os.getenv("IMAGE_BASE_URL", "http://localhost:8000")

# Проверяем наличие API ключей
if not FUSIONBRAIN_API_KEY or not FUSIONBRAIN_SECRET_KEY:
    logger.error("Missing API keys! Please set KANDINSKY_API_KEY and KANDINSKY_SECRET_KEY environment variables")
    logger.error("You can create a .env file based on .env.example")
    exit(1)

class GenerateRequest(BaseModel):
    type: str = "GENERATE"
    style: str = "DEFAULT"
    width: int = 1024
    height: int = 1024
    numImages: int = 1
    negativePromptDecoder: str = ""
    generateParams: Dict[str, Any]

class GenerateResponse(BaseModel):
    result: str
    imageUrl: Optional[str] = None
    error: Optional[str] = None

class FusionBrainAPI:
    """Класс для работы с FusionBrain API"""
    
    def __init__(self, url: str, api_key: str, secret_key: str):
        self.URL = url
        self.AUTH_HEADERS = {
            'X-Key': f'Key {api_key}',
            'X-Secret': f'Secret {secret_key}',
        }
    
    async def get_pipeline(self, session: aiohttp.ClientSession) -> str:
        """Получает ID пайплайна"""
        try:
            async with session.get(self.URL + 'key/api/v1/pipelines', headers=self.AUTH_HEADERS) as response:
                if response.status != 200:
                    raise Exception(f"Failed to get pipeline: {response.status}")
                data = await response.json()
                return data[0]['id']
        except Exception as e:
            logger.error(f"Error getting pipeline: {e}")
            raise
    
    async def generate(self, session: aiohttp.ClientSession, prompt: str, pipeline_id: str, 
                      images: int = 1, width: int = 1024, height: int = 1024) -> str:
        """Запускает генерацию изображения"""
        try:
            params = {
                "type": "GENERATE",
                "numImages": images,
                "width": width,
                "height": height,
                "generateParams": {
                    "query": prompt
                }
            }
            
            data = aiohttp.FormData()
            data.add_field('pipeline_id', pipeline_id)
            data.add_field('params', json.dumps(params), content_type='application/json')
            
            async with session.post(self.URL + 'key/api/v1/pipeline/run', 
                                  headers=self.AUTH_HEADERS, data=data) as response:
                # 201 означает "Created" - это успешный ответ
                if response.status not in [200, 201]:
                    raise Exception(f"Failed to start generation: {response.status}")
                result = await response.json()
                return result['uuid']
        except Exception as e:
            logger.error(f"Error starting generation: {e}")
            raise
    
    async def check_generation(self, session: aiohttp.ClientSession, request_id: str, 
                              attempts: int = 10, delay: int = 10) -> Optional[str]:
        """Проверяет статус генерации и возвращает base64 изображение"""
        while attempts > 0:
            try:
                async with session.get(self.URL + 'key/api/v1/pipeline/status/' + request_id, 
                                     headers=self.AUTH_HEADERS) as response:
                    if response.status == 200:
                        data = await response.json()
                        logger.info(f"Generation status: {data['status']}")
                        
                        if data['status'] == 'DONE':
                            # FusionBrain возвращает base64 изображение
                            if 'result' in data and 'files' in data['result']:
                                files = data['result']['files']
                                if files and len(files) > 0:
                                    # Первый файл содержит base64 изображение
                                    return files[0]
                        elif data['status'] == 'FAILED':
                            logger.error(f"Generation failed: {data}")
                            return None
                        
                        attempts -= 1
                        await asyncio.sleep(delay)
                    else:
                        logger.error(f"Failed to check status: {response.status}")
                        attempts -= 1
                        await asyncio.sleep(delay)
            except Exception as e:
                logger.error(f"Error checking generation status: {e}")
                attempts -= 1
                await asyncio.sleep(delay)
        
        return None
    


@app.post("/generate", response_model=GenerateResponse)
async def generate_image(request: GenerateRequest):
    """Генерирует изображение используя FusionBrain API"""
    try:
        logger.info(f"Received image generation request: {request.generateParams}")
        
        # Извлекаем промпт из запроса
        prompt = request.generateParams.get("query", "")
        if not prompt:
            raise HTTPException(status_code=400, detail="Query parameter is required")
        
        # Создаем временную папку для изображения
        temp_dir = tempfile.mkdtemp()
        image_filename = f"fusionbrain_{hash(prompt) % 10000}.png"
        image_path = os.path.join(temp_dir, image_filename)
        
        try:
            # Пытаемся использовать FusionBrain API
            logger.info("Calling FusionBrain API...")
            return await generate_fusionbrain_image(prompt, request.style)
        except Exception as e:
            logger.error(f"Error calling FusionBrain API: {e}")
            logger.info("Falling back to demo generation...")
            
            # Fallback на демо-генерацию
            try:
                result = await generate_demo_image(prompt, image_path, request.style)
                logger.info(f"Demo image generated successfully: {image_path}")
                
                # Копируем в финальную папку
                final_path = os.path.join("/tmp/android_images", image_filename)
                shutil.copy2(image_path, final_path)
                
                return GenerateResponse(
                    result="Demo image generated successfully",
                    imageUrl=f"{IMAGE_BASE_URL}/images/{image_filename}"
                )
            except Exception as demo_error:
                logger.error(f"Demo generation also failed: {demo_error}")
                raise HTTPException(status_code=500, detail=f"Both API and demo generation failed: {str(e)}")
            
        finally:
            # Очищаем временную папку
            shutil.rmtree(temp_dir, ignore_errors=True)
            
    except Exception as e:
        logger.error(f"Error generating FusionBrain image: {str(e)}")
        return GenerateResponse(
            result="Error generating FusionBrain image",
            error=str(e)
        )

async def generate_fusionbrain_image(prompt: str, style: str = "DEFAULT") -> GenerateResponse:
    """Генерирует изображение через FusionBrain API"""
    try:
        logger.info(f"Generating FusionBrain image with prompt: {prompt}")
        
        # Создаем API клиент
        api = FusionBrainAPI(
            url="https://api-key.fusionbrain.ai/",
            api_key=FUSIONBRAIN_API_KEY,
            secret_key=FUSIONBRAIN_SECRET_KEY
        )
        
        async with aiohttp.ClientSession() as session:
            # Получаем pipeline ID
            logger.info("Getting pipeline...")
            pipeline_id = await api.get_pipeline(session)
            logger.info(f"Pipeline ID: {pipeline_id}")
            
            # Запускаем генерацию
            logger.info("Starting image generation...")
            uuid = await api.generate(session, prompt, pipeline_id, width=1024, height=1024)
            logger.info(f"Generation started with UUID: {uuid}")
            
            # Ждем завершения генерации
            logger.info("Waiting for generation to complete...")
            base64_image = await api.check_generation(session, uuid, attempts=30, delay=5)
            logger.info(f"Generation completed, got base64 image")
            
            if not base64_image:
                raise Exception("No image generated")
            
            # Декодируем base64 изображение и сохраняем
            try:
                import base64
                image_data = base64.b64decode(base64_image)
                
                # Сохраняем изображение
                image_filename = f"fusionbrain_{int(time.time())}.png"
                image_path = os.path.join("/tmp/android_images", image_filename)
                
                with open(image_path, "wb") as f:
                    f.write(image_data)
                
                logger.info(f"FusionBrain image saved to: {image_path}")
                
                return GenerateResponse(
                    result="FusionBrain image generated successfully",
                    imageUrl=f"{IMAGE_BASE_URL}/images/{image_filename}"
                )
                
            except Exception as e:
                logger.error(f"Error saving base64 image: {e}")
                raise Exception(f"Failed to save image: {e}")
                
    except Exception as e:
        logger.error(f"Error generating FusionBrain image: {e}")
        raise

@app.get("/health")
async def health_check():
    """Проверка состояния сервера"""
    return {"status": "healthy", "service": "MCP FusionBrain Server"}

@app.get("/")
async def root():
    """Корневой endpoint"""
    return {
        "message": "MCP FusionBrain Server for Android",
        "endpoints": {
            "generate": "/generate",
            "health": "/health"
        }
    }



if __name__ == "__main__":
    # Создаем папку для изображений
    os.makedirs("/tmp/android_images", exist_ok=True)
    
    # Запускаем сервер
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )

package com.tayrinn.aiadvent.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.tayrinn.aiadvent.data.api.KandinskyApi
import com.tayrinn.aiadvent.data.api.KandinskyRequest
import com.tayrinn.aiadvent.data.api.GenerateParams
import com.tayrinn.aiadvent.data.model.ApiLimits
import com.tayrinn.aiadvent.data.preferences.ApiLimitsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ImageGenerationService(
                private val kandinskyApi: KandinskyApi,
                private val context: Context,
                private val apiLimitsPreferences: ApiLimitsPreferences
            ) {
    
    companion object {
        private const val TAG = "ImageGenerationService"
        private const val IMAGE_DIR = "generated_images"
    }
    
    // Создаем собственный ExecutorService для тяжелых операций
    private val executor = Executors.newFixedThreadPool(2)
    
    suspend fun generateImage(prompt: String, style: String = "DEFAULT"): Result<String> = coroutineScope {
        try {
            Log.d(TAG, "Generating image with prompt: $prompt")
            Log.d(TAG, "Current thread: ${Thread.currentThread().name}")
            
            // Запускаем API вызов в отдельной корутине
            val apiCall = async(Dispatchers.IO) {
                Log.d(TAG, "API call thread: ${Thread.currentThread().name}")
                val request = KandinskyRequest(
                    style = style,
                    generateParams = GenerateParams(query = prompt)
                )
                kandinskyApi.generateImage(request)
            }
            
            // Ждем результат API
            val response = apiCall.await()
            Log.d(TAG, "API response received")
            
            if (response.error != null) {
                Log.e(TAG, "Error generating image: ${response.error}")
                return@coroutineScope Result.failure(Exception(response.error))
            }
            
            // Запускаем сохранение изображения в отдельной корутине
            val saveCall = async(Dispatchers.IO) {
                Log.d(TAG, "Save call thread: ${Thread.currentThread().name}")
                saveImageLocally(response.imageUrl, prompt)
            }
            
            // Ждем результат сохранения
            val imagePath = saveCall.await()
            
            Log.d(TAG, "Image generated successfully: $imagePath")
            Result.success(imagePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate image", e)
            Result.failure(e)
        }
    }
    
    private suspend fun saveImageLocally(imageUrl: String?, prompt: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "saveImageLocally - Current thread: ${Thread.currentThread().name}")
            
            val imageDir = File(context.filesDir, IMAGE_DIR).apply {
                if (!exists()) mkdirs()
            }
            
            val fileName = "img_${System.currentTimeMillis()}.png"
            val imageFile = File(imageDir, fileName)
            
            // Загружаем изображение по URL с очень агрессивными таймаутами
            Log.d(TAG, "Downloading image from: $imageUrl")
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)   // Уменьшаем до 5 секунд
                .readTimeout(10, TimeUnit.SECONDS)     // Уменьшаем до 10 секунд
                .writeTimeout(5, TimeUnit.SECONDS)     // Уменьшаем до 5 секунд
                .build()
                
            val request = Request.Builder().url(imageUrl!!).build()
            
            // Используем ExecutorService для HTTP вызова
            val future = executor.submit<Pair<Boolean, String>> {
                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@submit Pair(false, "Failed to download image: ${response.code}")
                        }
                        
                        Log.d(TAG, "Image downloaded, decoding bitmap...")
                        val inputStream = response.body?.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        
                        if (bitmap == null) {
                            return@submit Pair(false, "Failed to decode bitmap")
                        }
                        
                        // Сохраняем изображение
                        Log.d(TAG, "Saving bitmap to file...")
                        FileOutputStream(imageFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        
                        inputStream?.close()
                        Log.d(TAG, "Image saved successfully to: ${imageFile.absolutePath}")
                        Pair(true, imageFile.absolutePath)
                    }
                } catch (e: Exception) {
                    Pair(false, "Exception: ${e.message}")
                }
            }
            
            // Ждем результат с таймаутом
            val result = try {
                future.get(15, TimeUnit.SECONDS) // 15 секунд таймаут
            } catch (e: Exception) {
                future.cancel(true)
                throw IOException("Operation timed out: ${e.message}")
            }
            
            if (!result.first) {
                throw IOException(result.second)
            }
            
            result.second
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image locally", e)
            throw e
        }
    }
    
    fun getImageFile(path: String): File? {
        val file = File(path)
        return if (file.exists()) file else null
    }
    
    fun clearOldImages() {
        try {
            val imageDir = File(context.filesDir, IMAGE_DIR)
            if (imageDir.exists()) {
                val files = imageDir.listFiles()
                files?.forEach { file ->
                    if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear old images", e)
        }
    }
    
    fun shutdown() {
        try {
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down executor", e)
            executor.shutdownNow()
        }
    }
    
                    suspend fun getApiLimits(): Result<ApiLimits> = withContext(Dispatchers.IO) {
                    try {
                        Log.d(TAG, "Getting API limits from local storage...")
                        val remaining = apiLimitsPreferences.getRemainingGenerations()
                        val total = apiLimitsPreferences.getTotalGenerations()
                        val resetDate = apiLimitsPreferences.getResetDate()
                        
                        val limits = ApiLimits(remaining, total, resetDate)
                        Log.d(TAG, "API limits loaded: ${limits.remainingGenerations}/${limits.totalGenerations}")
                        Result.success(limits)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get API limits", e)
                        Result.failure(e)
                    }
                }
                
                suspend fun decreaseRemainingGenerations(): Result<Unit> = withContext(Dispatchers.IO) {
                    try {
                        Log.d(TAG, "Decreasing remaining generations count")
                        apiLimitsPreferences.decreaseRemainingGenerations()
                        Log.d(TAG, "Remaining generations decreased to: ${apiLimitsPreferences.getRemainingGenerations()}")
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to decrease generations count", e)
                        Result.failure(e)
                    }
                }
}

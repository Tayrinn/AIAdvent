package com.tayrinn.aiadvent.di

import android.content.Context
import com.tayrinn.aiadvent.data.api.OllamaApi
import com.tayrinn.aiadvent.data.api.KandinskyApi
import com.tayrinn.aiadvent.data.database.ChatDatabase
import com.tayrinn.aiadvent.data.database.ChatMessageDao
import com.tayrinn.aiadvent.data.preferences.AppPreferences
import com.tayrinn.aiadvent.data.repository.ChatRepository
import com.tayrinn.aiadvent.data.service.ImageGenerationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideOllamaApi(appPreferences: AppPreferences): OllamaApi {
        val baseUrl = "http://${appPreferences.getOllamaIp()}:${appPreferences.getOllamaPort()}/"
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OllamaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKandinskyApi(appPreferences: AppPreferences): KandinskyApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)  // Уменьшаем с 60 до 15 секунд
            .readTimeout(30, TimeUnit.SECONDS)     // Уменьшаем с 60 до 30 секунд
            .writeTimeout(15, TimeUnit.SECONDS)    // Уменьшаем с 60 до 15 секунд
            .build()

        // Определяем URL в зависимости от того, запущено ли приложение в эмуляторе или на реальном устройстве
        val baseUrl = if (appPreferences.isEmulator()) {
            "http://10.0.2.2:8000/" // IP для Android эмулятора
        } else {
            "http://${appPreferences.getHostIp()}:8000/" // IP хоста для реального устройства
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KandinskyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {
        return ChatDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: ChatDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    @Singleton
    fun provideImageGenerationService(
        kandinskyApi: KandinskyApi,
        @ApplicationContext context: Context
    ): ImageGenerationService {
        return ImageGenerationService(kandinskyApi, context)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        ollamaApi: OllamaApi,
        chatMessageDao: ChatMessageDao,
        imageGenerationService: ImageGenerationService
    ): ChatRepository {
        return ChatRepository(ollamaApi, chatMessageDao, imageGenerationService)
    }
}

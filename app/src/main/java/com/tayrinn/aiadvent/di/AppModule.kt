package com.tayrinn.aiadvent.di

import android.content.Context
import com.tayrinn.aiadvent.data.api.OllamaApi
import com.tayrinn.aiadvent.data.preferences.AppPreferences
import com.tayrinn.aiadvent.data.database.ChatDatabase
import com.tayrinn.aiadvent.data.database.ChatMessageDao
import com.tayrinn.aiadvent.data.repository.ChatRepository
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
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOllamaApi(okHttpClient: OkHttpClient, appPreferences: AppPreferences): OllamaApi {
        val baseUrl = appPreferences.getOllamaBaseUrl()
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OllamaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        chatMessageDao: ChatMessageDao,
        ollamaApi: OllamaApi
    ): ChatRepository {
        return ChatRepository(chatMessageDao, ollamaApi)
    }
}

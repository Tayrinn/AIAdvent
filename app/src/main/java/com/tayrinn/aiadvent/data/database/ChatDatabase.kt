package com.tayrinn.aiadvent.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.tayrinn.aiadvent.data.model.ChatMessage

@Database(entities = [ChatMessage::class], version = 4, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        // Миграция с версии 1 на версию 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Добавляем новые колонки для агентов
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN isAgent1 INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN isAgent2 INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Миграция с версии 2 на версию 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Добавляем новые колонки для изображений
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN isImageGeneration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN imagePrompt TEXT")
            }
        }

        // Миграция с версии 3 на версию 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Добавляем новую колонку для отчетов о тестах
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN isTestReport INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration() // Удаляет базу при проблемах с миграцией
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * При проблемах с миграцией база данных будет автоматически пересоздана.
         * Это означает, что все данные будут потеряны, но приложение не упадет.
         * 
         * Если нужно сохранить данные, можно:
         * 1. Увеличить версию базы данных
         * 2. Добавить соответствующую миграцию
         * 3. Или временно отключить fallbackToDestructiveMigration()
         */
    }
}

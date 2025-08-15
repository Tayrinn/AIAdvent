package com.tayrinn.aiadvent.data.database

import androidx.room.*
import com.tayrinn.aiadvent.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import android.util.Log

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<ChatMessage> {
        Log.d("ChatMessageDao", "getAllMessagesSync called")
        try {
            Log.d("ChatMessageDao", "About to execute query")
            val messages = getAllMessagesSyncInternal()
            Log.d("ChatMessageDao", "Query executed successfully, got ${messages.size} messages")
            return messages
        } catch (e: Exception) {
            Log.e("ChatMessageDao", "Error in getAllMessagesSync: ${e.message}")
            throw e
        }
    }

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSyncInternal(): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Delete
    suspend fun deleteMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}

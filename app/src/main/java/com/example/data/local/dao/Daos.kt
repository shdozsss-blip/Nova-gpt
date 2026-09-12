package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AdminLogEntity
import com.example.data.local.entity.AiModelEntity
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesList(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM conversations")
    fun getConversationCount(): Flow<Int>
}

@Dao
interface AiModelDao {
    @Query("SELECT * FROM ai_models ORDER BY orderIndex ASC, name ASC")
    fun getAllModels(): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models WHERE isEnabled = 1 ORDER BY orderIndex ASC, name ASC")
    fun getEnabledModels(): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): AiModelEntity?

    @Query("SELECT * FROM ai_models WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultModel(): AiModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: AiModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<AiModelEntity>)

    @Update
    suspend fun updateModel(model: AiModelEntity)

    @Query("UPDATE ai_models SET isDefault = 0")
    suspend fun clearDefaultModel()

    @Query("DELETE FROM ai_models WHERE id = :id")
    suspend fun deleteModel(id: String)

    @Query("SELECT COUNT(*) FROM ai_models WHERE isEnabled = 1")
    fun getEnabledModelCount(): Flow<Int>
}

@Dao
interface AdminLogDao {
    @Query("SELECT * FROM admin_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AdminLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AdminLogEntity)

    @Query("DELETE FROM admin_logs")
    suspend fun clearLogs()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingEntity>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)
}

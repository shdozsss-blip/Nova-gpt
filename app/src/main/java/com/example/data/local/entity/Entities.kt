package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "openai/gpt-4o",
    val systemPrompt: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val imageUri: String? = null,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val isLiked: Boolean? = null // null: none, true: liked, false: disliked
)

@Entity(tableName = "ai_models")
data class AiModelEntity(
    @PrimaryKey val id: String, // e.g. "openai/gpt-4o"
    val name: String, // e.g. "GPT-4o"
    val provider: String, // e.g. "OpenAI"
    val description: String,
    val iconLabel: String = "AI",
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val orderIndex: Int = 0,
    val supportsVision: Boolean = true
)

@Entity(tableName = "admin_logs")
data class AdminLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

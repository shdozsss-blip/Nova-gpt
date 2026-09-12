package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AdminLogDao
import com.example.data.local.dao.AiModelDao
import com.example.data.local.dao.AppSettingDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.AdminLogEntity
import com.example.data.local.entity.AiModelEntity
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AiModelEntity::class,
        AdminLogEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun aiModelDao(): AiModelDao
    abstract fun adminLogDao(): AdminLogDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nova_chat_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val defaultModels = listOf(
                AiModelEntity(
                    id = "openai/gpt-4o",
                    name = "GPT-4o",
                    provider = "OpenAI",
                    description = "Most capable multimodal flagship model with high intelligence and vision.",
                    iconLabel = "OpenAI",
                    isEnabled = true,
                    isDefault = true,
                    orderIndex = 1,
                    supportsVision = true
                ),
                AiModelEntity(
                    id = "anthropic/claude-3.5-sonnet",
                    name = "Claude 3.5 Sonnet",
                    provider = "Anthropic",
                    description = "Industry-leading coding, natural reasoning, and deep analytical capabilities.",
                    iconLabel = "Claude",
                    isEnabled = true,
                    isDefault = false,
                    orderIndex = 2,
                    supportsVision = true
                ),
                AiModelEntity(
                    id = "meta-llama/llama-3.1-70b-instruct",
                    name = "Llama 3.1 70B",
                    provider = "Meta",
                    description = "State-of-the-art open weights model with fast throughput.",
                    iconLabel = "Meta",
                    isEnabled = true,
                    isDefault = false,
                    orderIndex = 3,
                    supportsVision = false
                ),
                AiModelEntity(
                    id = "google/gemini-flash-1.5",
                    name = "Gemini 1.5 Flash",
                    provider = "Google",
                    description = "Ultra-fast multimodal model with massive 1M token context window.",
                    iconLabel = "Gemini",
                    isEnabled = true,
                    isDefault = false,
                    orderIndex = 4,
                    supportsVision = true
                ),
                AiModelEntity(
                    id = "mistralai/mistral-large",
                    name = "Mistral Large",
                    provider = "Mistral",
                    description = "Top-tier flagship reasoning model with multilingual fluency.",
                    iconLabel = "Mistral",
                    isEnabled = true,
                    isDefault = false,
                    orderIndex = 5,
                    supportsVision = false
                ),
                AiModelEntity(
                    id = "deepseek/deepseek-r1",
                    name = "DeepSeek R1",
                    provider = "DeepSeek",
                    description = "Open reasoning model optimized for mathematical and logical proofs.",
                    iconLabel = "DeepSeek",
                    isEnabled = true,
                    isDefault = false,
                    orderIndex = 6,
                    supportsVision = false
                )
            )
            database.aiModelDao().insertModels(defaultModels)

            // Initial demo conversation from the user's reference image
            val sampleConvId = "sample_conv_1"
            database.chatDao().insertConversation(
                ConversationEntity(
                    id = sampleConvId,
                    title = "General Chat",
                    createdAt = System.currentTimeMillis() - 3600000,
                    updatedAt = System.currentTimeMillis() - 60000,
                    modelId = "openai/gpt-4o"
                )
            )

            database.chatDao().insertMessage(
                MessageEntity(
                    id = "msg_1",
                    conversationId = sampleConvId,
                    role = "user",
                    content = "Explain quantum computing in simple terms",
                    timestamp = System.currentTimeMillis() - 300000
                )
            )

            database.chatDao().insertMessage(
                MessageEntity(
                    id = "msg_2",
                    conversationId = sampleConvId,
                    role = "assistant",
                    content = """Quantum computing is a type of computing that uses the principles of quantum mechanics, like superposition and entanglement, to process information in new ways. Unlike classical computers that use bits (0 or 1), quantum computers use qubits, which can be 0, 1, or both at the same time.

Here are the key concepts:
• **Superposition** – qubits can be 0 and 1 at the same time.
• **Entanglement** – qubits can be linked, even across distance.
• **Quantum gates** – control qubit states like logic gates in normal computers.

Here is a quick conceptual example in Python:
```python
# Quantum circuit conceptual simulation
qubits = [0, 1]
print(f"Superposition states active: {len(qubits)}")
```

This allows quantum computers to solve certain problems much faster, like complex calculations, cryptography, and drug discovery.""",
                    timestamp = System.currentTimeMillis() - 250000,
                    modelUsed = "openai/gpt-4o"
                )
            )

            // Log initialization
            database.adminLogDao().insertLog(
                AdminLogEntity(
                    action = "System Initialized",
                    details = "Nova Chatbot AI database initialized with default models and settings."
                )
            )
        }
    }
}

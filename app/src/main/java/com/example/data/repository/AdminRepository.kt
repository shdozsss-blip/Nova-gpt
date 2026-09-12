package com.example.data.repository

import com.example.data.local.dao.AdminLogDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.AdminLogEntity
import com.example.data.remote.ChatMessagePayload
import com.example.data.remote.OpenRouterRequest
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

class AdminRepository(
    private val adminLogDao: AdminLogDao,
    private val chatDao: ChatDao,
    private val settingsRepository: SettingsRepository
) {
    val allLogs: Flow<List<AdminLogEntity>> = adminLogDao.getAllLogs()
    val totalChats: Flow<Int> = chatDao.getConversationCount()

    suspend fun logActivity(action: String, details: String) {
        adminLogDao.insertLog(
            AdminLogEntity(
                action = action,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearLogs() {
        adminLogDao.clearLogs()
    }

    suspend fun testApiConnection(apiKey: String, modelId: String): Result<String> {
        return try {
            val keyToUse = apiKey.ifBlank { settingsRepository.openRouterApiKey.value }
            if (keyToUse.isBlank()) {
                return Result.failure(Exception("API key is empty. Please enter an OpenRouter API key."))
            }

            val request = OpenRouterRequest(
                model = modelId.ifBlank { "openai/gpt-4o" },
                messages = listOf(
                    ChatMessagePayload(role = "user", content = "Ping! Reply with 'OK'.")
                ),
                maxTokens = 10,
                temperature = 0.1
            )

            val response = RetrofitClient.openRouterService.createChatCompletion(
                authHeader = "Bearer $keyToUse",
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val reply = body?.choices?.firstOrNull()?.message?.content ?: "Connected"
                settingsRepository.setApiStatus("Connected")
                logActivity("API Tested", "Connection test succeeded for model: $modelId")
                Result.success(reply)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                settingsRepository.setApiStatus("Connection Error")
                logActivity("API Test Failed", "HTTP ${response.code()}: $errorMsg")
                Result.failure(Exception("API Error (${response.code()}): $errorMsg"))
            }
        } catch (e: Exception) {
            settingsRepository.setApiStatus("Connection Error")
            logActivity("API Test Failed", e.localizedMessage ?: "Unknown network error")
            Result.failure(e)
        }
    }
}

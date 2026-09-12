package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AdminLogEntity
import com.example.data.local.entity.AiModelEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.repository.AdminRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ModelRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val settingsRepository = SettingsRepository(application)
    val adminRepository = AdminRepository(database.adminLogDao(), database.chatDao(), settingsRepository)
    val modelRepository = ModelRepository(database.aiModelDao())
    val chatRepository = ChatRepository(database.chatDao(), settingsRepository, adminRepository)

    // Auth state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Dashboard Data
    val totalChats: StateFlow<Int> = adminRepository.totalChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allModels: StateFlow<List<AiModelEntity>> = modelRepository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledModelsCount: StateFlow<Int> = modelRepository.enabledCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val adminLogs: StateFlow<List<AdminLogEntity>> = adminRepository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConversations: StateFlow<List<ConversationEntity>> = chatRepository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // API Testing State
    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    private val _testApiResult = MutableStateFlow<String?>(null)
    val testApiResult: StateFlow<String?> = _testApiResult.asStateFlow()

    private val _testApiSuccess = MutableStateFlow<Boolean?>(null)
    val testApiSuccess: StateFlow<Boolean?> = _testApiSuccess.asStateFlow()

    fun login(user: String, pass: String): Boolean {
        val success = settingsRepository.verifyAdmin(user, pass)
        if (success) {
            _isLoggedIn.value = true
            _loginError.value = null
            viewModelScope.launch {
                adminRepository.logActivity("Admin Login", "Admin successfully authenticated.")
            }
        } else {
            _loginError.value = "Invalid username or password. Please try again."
        }
        return success
    }

    fun logout() {
        _isLoggedIn.value = false
        _loginError.value = null
        viewModelScope.launch {
            adminRepository.logActivity("Admin Logout", "Admin logged out.")
        }
    }

    fun updatePassword(newPass: String) {
        if (newPass.length >= 4) {
            settingsRepository.updateAdminPassword(newPass)
            viewModelScope.launch {
                adminRepository.logActivity("Password Changed", "Admin updated master security password.")
            }
        }
    }

    // Model Management
    fun addModel(
        id: String,
        name: String,
        provider: String,
        description: String,
        iconLabel: String,
        isEnabled: Boolean,
        isDefault: Boolean,
        supportsVision: Boolean
    ) {
        viewModelScope.launch {
            val entity = AiModelEntity(
                id = id.trim(),
                name = name.trim(),
                provider = provider.trim(),
                description = description.trim(),
                iconLabel = iconLabel.trim().ifBlank { provider },
                isEnabled = isEnabled,
                isDefault = isDefault,
                supportsVision = supportsVision
            )
            modelRepository.addModel(entity)
            adminRepository.logActivity("Model Added", "Added model '$name' ($id)")
        }
    }

    fun updateModel(model: AiModelEntity) {
        viewModelScope.launch {
            modelRepository.updateModel(model)
            adminRepository.logActivity("Model Updated", "Updated model '${model.name}'")
        }
    }

    fun toggleModel(id: String, enabled: Boolean) {
        viewModelScope.launch {
            modelRepository.toggleEnabled(id, enabled)
            adminRepository.logActivity(
                if (enabled) "Model Enabled" else "Model Disabled",
                "Model ID: $id"
            )
        }
    }

    fun setDefaultModel(id: String) {
        viewModelScope.launch {
            modelRepository.setDefaultModel(id)
            adminRepository.logActivity("Default Model Changed", "New default model: $id")
        }
    }

    fun deleteModel(id: String) {
        viewModelScope.launch {
            modelRepository.deleteModel(id)
            adminRepository.logActivity("Model Deleted", "Deleted model ID: $id")
        }
    }

    // API Management
    fun saveApiKey(key: String) {
        settingsRepository.saveOpenRouterApiKey(key)
        viewModelScope.launch {
            adminRepository.logActivity("API Key Updated", "OpenRouter API credentials saved.")
        }
    }

    fun setApiEnabled(enabled: Boolean) {
        settingsRepository.setApiEnabled(enabled)
        viewModelScope.launch {
            adminRepository.logActivity(
                if (enabled) "API Enabled" else "API Disabled",
                "OpenRouter API access toggled to $enabled"
            )
        }
    }

    fun testApiConnection(apiKey: String, modelId: String) {
        _isTestingApi.value = true
        _testApiResult.value = null
        _testApiSuccess.value = null

        viewModelScope.launch {
            val result = adminRepository.testApiConnection(apiKey, modelId)
            _isTestingApi.value = false
            if (result.isSuccess) {
                _testApiSuccess.value = true
                _testApiResult.value = "Connection Successful! Model responded: ${result.getOrNull()}"
            } else {
                _testApiSuccess.value = false
                _testApiResult.value = result.exceptionOrNull()?.localizedMessage ?: "Connection Failed"
            }
        }
    }

    fun saveApiParameters(temp: Float, maxTokens: Int, sysPrompt: String, streaming: Boolean) {
        settingsRepository.saveApiParameters(temp, maxTokens, sysPrompt, streaming)
        viewModelScope.launch {
            adminRepository.logActivity(
                "App Configuration Changed",
                "Temperature: $temp, MaxTokens: $maxTokens, Streaming: $streaming"
            )
        }
    }

    // Feature Toggles
    fun setFeature(key: String, enabled: Boolean) {
        settingsRepository.setFeatureToggle(key, enabled)
        viewModelScope.launch {
            adminRepository.logActivity("Feature Toggled", "Feature '$key' set to $enabled")
        }
    }

    // Chat Management
    fun renameChat(id: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateConversationTitle(id, newTitle)
            adminRepository.logActivity("Chat Renamed", "Chat $id renamed to '$newTitle'")
        }
    }

    fun deleteChat(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
        }
    }

    fun deleteAllChats() {
        viewModelScope.launch {
            chatRepository.deleteAllConversations()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            adminRepository.clearLogs()
        }
    }
}

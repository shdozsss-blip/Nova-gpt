package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_settings", Context.MODE_PRIVATE)

    // Admin Credentials
    val adminUsername = "shahniar"
    private val _adminPassword = MutableStateFlow(prefs.getString("admin_pwd", "shahniar2026") ?: "shahniar2026")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    // API Settings
    private val _openRouterApiKey = MutableStateFlow(prefs.getString("openrouter_api_key", "") ?: "")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _isApiEnabled = MutableStateFlow(prefs.getBoolean("is_api_enabled", true))
    val isApiEnabled: StateFlow<Boolean> = _isApiEnabled.asStateFlow()

    private val _apiStatus = MutableStateFlow(prefs.getString("api_status", "Not Configured") ?: "Not Configured")
    val apiStatus: StateFlow<String> = _apiStatus.asStateFlow()

    private val _temperature = MutableStateFlow(prefs.getFloat("temperature", 0.7f))
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _maxTokens = MutableStateFlow(prefs.getInt("max_tokens", 2048))
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _systemPrompt = MutableStateFlow(
        prefs.getString(
            "system_prompt",
            "You are Nova, an intelligent, helpful, and concise next-generation AI assistant. Format your responses with clean Markdown, clear headings, bullet points, and code blocks where appropriate."
        ) ?: ""
    )
    val systemPrompt: StateFlow<String> = _systemPrompt.asStateFlow()

    private val _streamingEnabled = MutableStateFlow(prefs.getBoolean("streaming_enabled", true))
    val streamingEnabled: StateFlow<Boolean> = _streamingEnabled.asStateFlow()

    // Appearance & Chat Behavior
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "dark") ?: "dark") // "dark", "light", "system"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _fontSize = MutableStateFlow(prefs.getString("font_size", "medium") ?: "medium") // "small", "medium", "large"
    val fontSize: StateFlow<String> = _fontSize.asStateFlow()

    private val _autoScroll = MutableStateFlow(prefs.getBoolean("auto_scroll", true))
    val autoScroll: StateFlow<Boolean> = _autoScroll.asStateFlow()

    // Feature Toggles (Admin Feature Control)
    private val _imageUploadEnabled = MutableStateFlow(prefs.getBoolean("feat_image_upload", true))
    val imageUploadEnabled: StateFlow<Boolean> = _imageUploadEnabled.asStateFlow()

    private val _cameraEnabled = MutableStateFlow(prefs.getBoolean("feat_camera", true))
    val cameraEnabled: StateFlow<Boolean> = _cameraEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("feat_notifications", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _voiceInputEnabled = MutableStateFlow(prefs.getBoolean("feat_voice_input", true))
    val voiceInputEnabled: StateFlow<Boolean> = _voiceInputEnabled.asStateFlow()

    private val _voiceOutputEnabled = MutableStateFlow(prefs.getBoolean("feat_voice_output", true))
    val voiceOutputEnabled: StateFlow<Boolean> = _voiceOutputEnabled.asStateFlow()

    private val _webSearchEnabled = MutableStateFlow(prefs.getBoolean("feat_web_search", false))
    val webSearchEnabled: StateFlow<Boolean> = _webSearchEnabled.asStateFlow()

    private val _imageGenEnabled = MutableStateFlow(prefs.getBoolean("feat_image_gen", true))
    val imageGenEnabled: StateFlow<Boolean> = _imageGenEnabled.asStateFlow()

    private val _codeToolsEnabled = MutableStateFlow(prefs.getBoolean("feat_code_tools", true))
    val codeToolsEnabled: StateFlow<Boolean> = _codeToolsEnabled.asStateFlow()

    private val _fileUploadEnabled = MutableStateFlow(prefs.getBoolean("feat_file_upload", true))
    val fileUploadEnabled: StateFlow<Boolean> = _fileUploadEnabled.asStateFlow()

    private val _modelSelectorEnabled = MutableStateFlow(prefs.getBoolean("feat_model_selector", true))
    val modelSelectorEnabled: StateFlow<Boolean> = _modelSelectorEnabled.asStateFlow()

    fun verifyAdmin(user: String, pass: String): Boolean {
        return user.trim().equals(adminUsername, ignoreCase = true) && pass.trim() == _adminPassword.value
    }

    fun updateAdminPassword(newPass: String) {
        prefs.edit().putString("admin_pwd", newPass).apply()
        _adminPassword.value = newPass
    }

    fun saveOpenRouterApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("openrouter_api_key", trimmed).apply()
        _openRouterApiKey.value = trimmed
        if (trimmed.isNotBlank()) {
            setApiStatus("Connected")
        } else {
            setApiStatus("Not Configured")
        }
    }

    fun setApiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_api_enabled", enabled).apply()
        _isApiEnabled.value = enabled
        if (!enabled) {
            setApiStatus("Disabled")
        } else if (_openRouterApiKey.value.isNotBlank()) {
            setApiStatus("Connected")
        } else {
            setApiStatus("Not Configured")
        }
    }

    fun setApiStatus(status: String) {
        prefs.edit().putString("api_status", status).apply()
        _apiStatus.value = status
    }

    fun saveApiParameters(temp: Float, maxToks: Int, prompt: String, stream: Boolean) {
        prefs.edit()
            .putFloat("temperature", temp)
            .putInt("max_tokens", maxToks)
            .putString("system_prompt", prompt)
            .putBoolean("streaming_enabled", stream)
            .apply()
        _temperature.value = temp
        _maxTokens.value = maxToks
        _systemPrompt.value = prompt
        _streamingEnabled.value = stream
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setFontSize(size: String) {
        prefs.edit().putString("font_size", size).apply()
        _fontSize.value = size
    }

    fun setAutoScroll(enabled: Boolean) {
        prefs.edit().putBoolean("auto_scroll", enabled).apply()
        _autoScroll.value = enabled
    }

    fun setFeatureToggle(featureKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(featureKey, enabled).apply()
        when (featureKey) {
            "feat_image_upload" -> _imageUploadEnabled.value = enabled
            "feat_camera" -> _cameraEnabled.value = enabled
            "feat_notifications" -> _notificationsEnabled.value = enabled
            "feat_voice_input" -> _voiceInputEnabled.value = enabled
            "feat_voice_output" -> _voiceOutputEnabled.value = enabled
            "feat_web_search" -> _webSearchEnabled.value = enabled
            "feat_image_gen" -> _imageGenEnabled.value = enabled
            "feat_code_tools" -> _codeToolsEnabled.value = enabled
            "feat_file_upload" -> _fileUploadEnabled.value = enabled
            "feat_model_selector" -> _modelSelectorEnabled.value = enabled
        }
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AiModelEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.AdminRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ModelRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    val database = AppDatabase.getDatabase(application, viewModelScope)
    val settingsRepository = SettingsRepository(application)
    val adminRepository = AdminRepository(database.adminLogDao(), database.chatDao(), settingsRepository)
    val modelRepository = ModelRepository(database.aiModelDao())
    val chatRepository = ChatRepository(database.chatDao(), settingsRepository, adminRepository)

    val allConversations: StateFlow<List<ConversationEntity>> = chatRepository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledModels: StateFlow<List<AiModelEntity>> = modelRepository.enabledModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _currentConversation = MutableStateFlow<ConversationEntity?>(null)
    val currentConversation: StateFlow<ConversationEntity?> = _currentConversation.asStateFlow()

    val messages: StateFlow<List<MessageEntity>> = _currentConversationId
        .flatMapLatest { id ->
            if (id != null) chatRepository.getMessages(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedModel = MutableStateFlow<AiModelEntity?>(null)
    val selectedModel: StateFlow<AiModelEntity?> = _selectedModel.asStateFlow()

    // Input state
    val inputText = MutableStateFlow("")
    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageBitmap = MutableStateFlow<Bitmap?>(null)

    // Generation State
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingMessage = MutableStateFlow<MessageEntity?>(null)
    val streamingMessage: StateFlow<MessageEntity?> = _streamingMessage.asStateFlow()

    private var activeGenerationJob: Job? = null

    init {
        viewModelScope.launch {
            // Select default model
            val defModel = modelRepository.getDefaultModel()
            _selectedModel.value = defModel

            // Load initial conversation
            allConversations.collect { convs ->
                if (_currentConversationId.value == null && convs.isNotEmpty()) {
                    selectConversation(convs.first().id)
                }
            }
        }
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
        viewModelScope.launch {
            val conv = chatRepository.getConversation(id)
            _currentConversation.value = conv
            if (conv != null) {
                val model = modelRepository.getModelById(conv.modelId)
                if (model != null) {
                    _selectedModel.value = model
                }
            }
        }
    }

    fun selectModel(model: AiModelEntity) {
        _selectedModel.value = model
        val conv = _currentConversation.value
        if (conv != null) {
            viewModelScope.launch {
                chatRepository.updateConversationTitle(conv.id, conv.title) // retains conv
                val updated = conv.copy(modelId = model.id)
                database.chatDao().updateConversation(updated)
                _currentConversation.value = updated
            }
        }
    }

    fun startNewChat(
        title: String = "New Chat",
        modelId: String? = null
    ) {
        viewModelScope.launch {
            val chosenModelId = modelId ?: _selectedModel.value?.id ?: "openai/gpt-4o"
            val conv = chatRepository.createConversation(title = title, modelId = chosenModelId)
            selectConversation(conv.id)
        }
    }

    fun sendMessage() {
        val text = inputText.value.trim()
        val uri = selectedImageUri.value
        val bitmap = selectedImageBitmap.value

        if (text.isBlank() && uri == null && bitmap == null) return
        if (_isGenerating.value) return

        val convId = _currentConversationId.value ?: return
        val currentModel = _selectedModel.value?.id ?: "openai/gpt-4o"

        var base64Image: String? = null
        if (bitmap != null) {
            try {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                // Ignore compression failure
            }
        }

        // Clear inputs
        inputText.value = ""
        selectedImageUri.value = null
        selectedImageBitmap.value = null

        viewModelScope.launch {
            // Save user message
            chatRepository.saveUserMessage(
                conversationId = convId,
                text = text.ifBlank { "Describe this image" },
                imageUri = uri?.toString(),
                imageBase64 = base64Image
            )

            // Start AI Generation
            generateAiResponse(
                conversationId = convId,
                modelId = currentModel,
                prompt = text,
                hasImage = (bitmap != null || uri != null)
            )
        }
    }

    private fun generateAiResponse(
        conversationId: String,
        modelId: String,
        prompt: String,
        hasImage: Boolean
    ) {
        activeGenerationJob?.cancel()
        _isGenerating.value = true

        val streamingMsgId = UUID.randomUUID().toString()
        val streamingMsg = MessageEntity(
            id = streamingMsgId,
            conversationId = conversationId,
            role = "assistant",
            content = "",
            modelUsed = modelId
        )
        _streamingMessage.value = streamingMsg

        activeGenerationJob = viewModelScope.launch {
            try {
                chatRepository.executeAiRequest(
                    conversationId = conversationId,
                    modelId = modelId,
                    userPrompt = prompt,
                    hasImage = hasImage,
                    onChunk = { textSoFar ->
                        _streamingMessage.value = streamingMsg.copy(content = textSoFar)
                    }
                )
            } catch (e: Exception) {
                // Handle cancellation or error
            } finally {
                _streamingMessage.value = null
                _isGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        val currentStream = _streamingMessage.value
        if (currentStream != null && currentStream.content.isNotBlank()) {
            viewModelScope.launch {
                chatRepository.saveAssistantMessage(
                    conversationId = currentStream.conversationId,
                    content = currentStream.content + " [Stopped]",
                    modelUsed = currentStream.modelUsed ?: "Nova"
                )
            }
        }
        _streamingMessage.value = null
        _isGenerating.value = false
    }

    fun regenerateLast() {
        val convId = _currentConversationId.value ?: return
        val currentList = messages.value
        val lastUserMsg = currentList.lastOrNull { it.role == "user" } ?: return
        val currentModel = _selectedModel.value?.id ?: "openai/gpt-4o"

        generateAiResponse(
            conversationId = convId,
            modelId = currentModel,
            prompt = lastUserMsg.content,
            hasImage = (!lastUserMsg.imageBase64.isNullOrBlank() || !lastUserMsg.imageUri.isNullOrBlank())
        )
    }

    fun setReaction(message: MessageEntity, liked: Boolean?) {
        viewModelScope.launch {
            chatRepository.setReaction(message, liked)
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateConversationTitle(id, newTitle)
            if (_currentConversationId.value == id) {
                _currentConversation.value = _currentConversation.value?.copy(title = newTitle)
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (_currentConversationId.value == id) {
                val remaining = allConversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first().id)
                } else {
                    _currentConversationId.value = null
                    _currentConversation.value = null
                }
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            chatRepository.deleteAllConversations()
            _currentConversationId.value = null
            _currentConversation.value = null
            startNewChat()
        }
    }

    fun setImageUri(uri: Uri?) {
        selectedImageUri.value = uri
        selectedImageBitmap.value = null
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        selectedImageBitmap.value = bitmap
        selectedImageUri.value = null
    }

    fun removeSelectedImage() {
        selectedImageUri.value = null
        selectedImageBitmap.value = null
    }
}

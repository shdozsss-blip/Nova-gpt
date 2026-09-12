package com.example.ui.screens.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.entity.AiModelEntity
import com.example.data.local.entity.MessageEntity
import com.example.ui.components.MessageBubble
import com.example.ui.components.NovaAvatar
import com.example.ui.components.NovaStarLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenDrawer: () -> Unit,
    onOpenNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentConversation by viewModel.currentConversation.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val streamingMessage by viewModel.streamingMessage.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val enabledModels by viewModel.enabledModels.collectAsState()

    val inputText by viewModel.inputText.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val selectedImageBitmap by viewModel.selectedImageBitmap.collectAsState()

    val autoScroll by viewModel.settingsRepository.autoScroll.collectAsState()
    val fontSizeStr by viewModel.settingsRepository.fontSize.collectAsState()
    val imageUploadEnabled by viewModel.settingsRepository.imageUploadEnabled.collectAsState()
    val cameraEnabled by viewModel.settingsRepository.cameraEnabled.collectAsState()
    val voiceInputEnabled by viewModel.settingsRepository.voiceInputEnabled.collectAsState()
    val modelSelectorEnabled by viewModel.settingsRepository.modelSelectorEnabled.collectAsState()

    val fontSize = when (fontSizeStr) {
        "small" -> 13.sp
        "large" -> 17.sp
        else -> 15.sp
    }

    var showAttachDialog by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    // Media and Camera pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setImageUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.setImageBitmap(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch()
        } else {
            permissionDeniedMessage = "Camera permission is required to take photos directly inside Nova."
        }
    }

    // Voice input launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.inputText.value = spokenText
            }
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, streamingMessage?.content) {
        if (autoScroll && (messages.isNotEmpty() || streamingMessage != null)) {
            val totalCount = messages.size + (if (streamingMessage != null) 1 else 0)
            if (totalCount > 0) {
                listState.animateScrollToItem(totalCount - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (modelSelectorEnabled) showModelMenu = true
                        }
                    ) {
                        NovaAvatar(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentConversation?.title ?: "Nova Chat",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedModel?.name ?: "GPT-4o",
                                    fontSize = 12.sp,
                                    color = NovaBlue,
                                    fontWeight = FontWeight.Medium
                                )
                                if (modelSelectorEnabled) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch Model",
                                        tint = NovaBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("chat_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // New Chat Action
                    IconButton(
                        onClick = onOpenNewChat,
                        modifier = Modifier.testTag("new_chat_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddComment,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // More Options
                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier.testTag("chat_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Options Dropdown
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename Chat") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                showOptionsMenu = false
                                renameInput = currentConversation?.title ?: ""
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Conversation") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                            onClick = {
                                showOptionsMenu = false
                                currentConversation?.let { viewModel.deleteConversation(it.id) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            onClick = {
                                showOptionsMenu = false
                                onOpenSettings()
                            }
                        )
                    }

                    // Model Selector Dropdown
                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false }
                    ) {
                        Text(
                            text = "Select Model",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider()
                        enabledModels.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${model.provider} • ${if (model.supportsVision) "Vision supported" else "Text only"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (model.id == selectedModel?.id) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = NovaBlue)
                                    }
                                },
                                onClick = {
                                    viewModel.selectModel(model)
                                    showModelMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Input Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Attached Image Preview
                AnimatedVisibility(
                    visible = (selectedImageUri != null || selectedImageBitmap != null)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(6.dp)
                        ) {
                            if (selectedImageBitmap != null) {
                                Image(
                                    bitmap = selectedImageBitmap!!.asImageBitmap(),
                                    contentDescription = "Selected image preview",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected image preview",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Image attached",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.removeSelectedImage() },
                                modifier = Modifier.size(28.dp).testTag("remove_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Chat Input Field and Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment button (+)
                    if (imageUploadEnabled) {
                        IconButton(
                            onClick = { showAttachDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("attach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach image",
                                tint = NovaBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Text Input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.inputText.value = it },
                        placeholder = {
                            Text(
                                "Type a message...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send or Stop or Mic button
                    if (isGenerating) {
                        // Stop generation button
                        IconButton(
                            onClick = { viewModel.stopGeneration() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(StatusRed)
                                .testTag("stop_generation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop response",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (inputText.isNotBlank() || selectedImageUri != null || selectedImageBitmap != null) {
                        // Send button
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(NovaButtonGradient)
                                .testTag("send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (voiceInputEnabled) {
                        // Mic voice input button
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Nova...")
                                }
                                try {
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    // Speech recognition not supported on device
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("voice_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (messages.isEmpty() && streamingMessage == null) {
                // Empty state greeting
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NovaStarLogo(size = 80.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nova Chatbot AI",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "How can I help you today?",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Suggestion prompt chips
                    val suggestions = listOf(
                        "Explain quantum computing in simple terms",
                        "Write a Python function to sort a list",
                        "What are the key differences between AI models?"
                    )

                    suggestions.forEach { prompt ->
                        Surface(
                            onClick = {
                                viewModel.inputText.value = prompt
                                viewModel.sendMessage()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            } else {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            isStreamingThisMessage = false,
                            fontSize = fontSize,
                            onLike = { liked -> viewModel.setReaction(msg, liked) },
                            onRegenerate = { viewModel.regenerateLast() }
                        )
                    }

                    // Actively streaming message
                    streamingMessage?.let { streamMsg ->
                        item(key = streamMsg.id) {
                            MessageBubble(
                                message = streamMsg,
                                isStreamingThisMessage = true,
                                fontSize = fontSize
                            )
                        }
                    }
                }
            }
        }
    }

    // Image Attachment Dialog (Camera / Gallery)
    if (showAttachDialog) {
        AlertDialog(
            onDismissRequest = { showAttachDialog = false },
            title = { Text("Attach Image") },
            text = { Text("Select an image from your gallery or capture a new photo with camera.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAttachDialog = false
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                ) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                if (cameraEnabled) {
                    TextButton(
                        onClick = {
                            showAttachDialog = false
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                cameraLauncher.launch()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Text("Camera")
                    }
                }
            }
        )
    }

    // Permission Denied Dialog
    permissionDeniedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { permissionDeniedMessage = null },
            title = { Text("Permission Notice") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { permissionDeniedMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Rename Chat Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Chat Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentConversation?.let {
                            viewModel.renameConversation(it.id, renameInput.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

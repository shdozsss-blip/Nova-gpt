package com.example.ui.screens.newchat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AiModelEntity
import com.example.ui.components.NovaAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatModal(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledModels by viewModel.enabledModels.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    var activeModel by remember(selectedModel) { mutableStateOf(selectedModel) }
    var webSearchEnabled by remember { mutableStateOf(false) }
    var imageGenEnabled by remember { mutableStateOf(false) }
    var codeInterpreterEnabled by remember { mutableStateOf(true) }

    var showModelDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        activeModel?.let { viewModel.selectModel(it) }
                        viewModel.startNewChat(
                            title = "New Chat",
                            modelId = activeModel?.id
                        )
                        onStartChat()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .testTag("start_new_chat_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NovaButtonGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start New Chat",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Select Model Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Model",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "OpenRouter",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NovaBlue
                    )
                }

                // Selected Model Card (Dropdown trigger)
                Surface(
                    onClick = { showModelDropdown = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NovaAvatar(size = 38.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeModel?.name ?: "GPT-4o",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeModel?.id ?: "openai/gpt-4o",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dropdown menu of models
                DropdownMenu(
                    expanded = showModelDropdown,
                    onDismissRequest = { showModelDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    enabledModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(model.name, fontWeight = FontWeight.Bold)
                                    Text(model.id, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            trailingIcon = {
                                if (model.id == activeModel?.id) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NovaBlue)
                                }
                            },
                            onClick = {
                                activeModel = model
                                showModelDropdown = false
                            }
                        )
                    }
                }

                Text(
                    text = "More models available in settings",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // AI Capabilities Toggles
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Web Search
                CapabilityToggleItem(
                    icon = Icons.Outlined.TravelExplore,
                    title = "Web Search",
                    description = "Get real-time information (slower)",
                    checked = webSearchEnabled,
                    onCheckedChange = { webSearchEnabled = it }
                )

                // Image Generation
                CapabilityToggleItem(
                    icon = Icons.Outlined.Image,
                    title = "Image Generation",
                    description = "Generate images with AI",
                    checked = imageGenEnabled,
                    onCheckedChange = { imageGenEnabled = it }
                )

                // Code Interpreter
                CapabilityToggleItem(
                    icon = Icons.Outlined.Code,
                    title = "Code Interpreter",
                    description = "Run code & analyze data",
                    checked = codeInterpreterEnabled,
                    onCheckedChange = { codeInterpreterEnabled = it }
                )
            }
        }
    }
}

@Composable
fun CapabilityToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NovaBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NovaBlue
                )
            )
        }
    }
}

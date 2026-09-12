package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeMode by viewModel.settingsRepository.themeMode.collectAsState()
    val autoScroll by viewModel.settingsRepository.autoScroll.collectAsState()
    val streamingEnabled by viewModel.settingsRepository.streamingEnabled.collectAsState()
    val fontSize by viewModel.settingsRepository.fontSize.collectAsState()
    val imageUploadEnabled by viewModel.settingsRepository.imageUploadEnabled.collectAsState()
    val notificationsEnabled by viewModel.settingsRepository.notificationsEnabled.collectAsState()

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsToggleRow(
                    icon = if (themeMode == "dark") Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    title = if (themeMode == "dark") "Dark Mode" else "Light Mode",
                    checked = themeMode == "dark",
                    onCheckedChange = { isDark ->
                        viewModel.settingsRepository.setThemeMode(if (isDark) "dark" else "light")
                    }
                )
            }

            // Chat Settings Section
            SettingsSection(title = "Chat Settings") {
                SettingsToggleRow(
                    icon = Icons.Outlined.AutoMode,
                    title = "Auto-scroll",
                    checked = autoScroll,
                    onCheckedChange = { viewModel.settingsRepository.setAutoScroll(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsToggleRow(
                    icon = Icons.Outlined.Animation,
                    title = "Streaming animation",
                    checked = streamingEnabled,
                    onCheckedChange = {
                        viewModel.settingsRepository.saveApiParameters(
                            viewModel.settingsRepository.temperature.value,
                            viewModel.settingsRepository.maxTokens.value,
                            viewModel.settingsRepository.systemPrompt.value,
                            it
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsActionRow(
                    icon = Icons.Outlined.FormatSize,
                    title = "Font Size",
                    value = fontSize.replaceFirstChar { it.uppercase() },
                    onClick = { showFontSizeDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsActionRow(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "Message Style",
                    value = "ChatGPT >",
                    onClick = { /* informative */ }
                )
            }

            // Data & Storage Section
            SettingsSection(title = "Data & Storage") {
                SettingsActionRow(
                    icon = Icons.Outlined.History,
                    title = "Chat History",
                    value = ">",
                    onClick = onNavigateToHistory
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsActionRow(
                    icon = Icons.Outlined.Share,
                    title = "Export Chats",
                    value = ">",
                    onClick = {
                        val allConvs = viewModel.allConversations.value
                        val exportText = buildString {
                            appendLine("=== Nova Chatbot AI - Exported Chats ===")
                            allConvs.forEach { conv ->
                                appendLine("Conversation: ${conv.title} (Model: ${conv.modelId})")
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, exportText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Chats"))
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsActionRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Clear All Data",
                    value = ">",
                    titleColor = StatusRed,
                    onClick = { showClearDataDialog = true }
                )
            }

            // Features Section
            SettingsSection(title = "Features") {
                SettingsToggleRow(
                    icon = Icons.Outlined.Image,
                    title = "Image Upload",
                    checked = imageUploadEnabled,
                    onCheckedChange = { viewModel.settingsRepository.setFeatureToggle("feat_image_upload", it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                SettingsToggleRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.settingsRepository.setFeatureToggle("feat_notifications", it) }
                )
            }

            // Admin Panel Access Section
            SettingsSection(title = "Administration") {
                SettingsActionRow(
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = "Admin Panel",
                    value = "Enter >",
                    titleColor = NovaPurple,
                    onClick = onNavigateToAdmin
                )
            }

            // About Section
            SettingsSection(title = "About") {
                SettingsActionRow(
                    icon = Icons.Outlined.Info,
                    title = "Nova Chatbot AI",
                    value = "Version 1.0.0",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("This will permanently delete all conversation history and messages from local storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllConversations()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Clear Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Font Size Selection Dialog
    if (showFontSizeDialog) {
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            title = { Text("Select Font Size") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("small", "medium", "large").forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.settingsRepository.setFontSize(size)
                                    showFontSizeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (fontSize == size),
                                onClick = {
                                    viewModel.settingsRepository.setFontSize(size)
                                    showFontSizeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(size.replaceFirstChar { it.uppercase() }, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontSizeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NovaBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
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

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    value: String = "",
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor != MaterialTheme.colorScheme.onSurface) titleColor else NovaBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

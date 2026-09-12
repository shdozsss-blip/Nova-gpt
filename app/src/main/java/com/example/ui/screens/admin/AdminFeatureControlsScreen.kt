package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeatureControlsScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imgUpload by adminViewModel.settingsRepository.imageUploadEnabled.collectAsState()
    val camera by adminViewModel.settingsRepository.cameraEnabled.collectAsState()
    val notifs by adminViewModel.settingsRepository.notificationsEnabled.collectAsState()
    val voiceIn by adminViewModel.settingsRepository.voiceInputEnabled.collectAsState()
    val voiceOut by adminViewModel.settingsRepository.voiceOutputEnabled.collectAsState()
    val webSearch by adminViewModel.settingsRepository.webSearchEnabled.collectAsState()
    val imgGen by adminViewModel.settingsRepository.imageGenEnabled.collectAsState()
    val fileUp by adminViewModel.settingsRepository.fileUploadEnabled.collectAsState()
    val codeTools by adminViewModel.settingsRepository.codeToolsEnabled.collectAsState()
    val modelSelector by adminViewModel.settingsRepository.modelSelectorEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feature Controls", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Toggle features and capabilities globally across Nova AI",
                fontSize = 13.sp,
                color = DarkTextSecondary
            )

            FeatureControlRow(
                icon = Icons.Outlined.Image,
                title = "Image Upload",
                description = "Allow users to upload images from photo gallery",
                checked = imgUpload,
                onCheckedChange = { adminViewModel.setFeature("feat_image_upload", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.PhotoCamera,
                title = "Camera Capture",
                description = "Allow users to take photos directly inside Nova",
                checked = camera,
                onCheckedChange = { adminViewModel.setFeature("feat_camera", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.Notifications,
                title = "Push Notifications",
                description = "Send app notifications and generation status alerts",
                checked = notifs,
                onCheckedChange = { adminViewModel.setFeature("feat_notifications", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.Mic,
                title = "Voice Input",
                description = "Enable speech-to-text microphone input on chat bar",
                checked = voiceIn,
                onCheckedChange = { adminViewModel.setFeature("feat_voice_input", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.VolumeUp,
                title = "Voice Output",
                description = "Allow text-to-speech reading of AI messages",
                checked = voiceOut,
                onCheckedChange = { adminViewModel.setFeature("feat_voice_output", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.TravelExplore,
                title = "Web Search",
                description = "Enable internet search capability for live queries",
                checked = webSearch,
                onCheckedChange = { adminViewModel.setFeature("feat_web_search", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.Brush,
                title = "Image Generation",
                description = "Enable image generation prompts & tools",
                checked = imgGen,
                onCheckedChange = { adminViewModel.setFeature("feat_image_gen", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.AttachFile,
                title = "Document & File Upload",
                description = "Allow attaching PDFs, text files, and spreadsheets",
                checked = fileUp,
                onCheckedChange = { adminViewModel.setFeature("feat_file_upload", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.Terminal,
                title = "Code Execution & Tools",
                description = "Syntax highlighting, copy code, and interpreter tools",
                checked = codeTools,
                onCheckedChange = { adminViewModel.setFeature("feat_code_tools", it) }
            )

            FeatureControlRow(
                icon = Icons.Outlined.SwapHoriz,
                title = "Model Selector",
                description = "Show model selector dropdown on chat screen",
                checked = modelSelector,
                onCheckedChange = { adminViewModel.setFeature("feat_model_selector", it) }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun FeatureControlRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (checked) NovaBlue.copy(alpha = 0.15f) else DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) NovaBlue else DarkTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(description, fontSize = 11.sp, color = DarkTextSecondary)
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

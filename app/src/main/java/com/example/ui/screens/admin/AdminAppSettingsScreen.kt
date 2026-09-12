package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppSettingsScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTemp by adminViewModel.settingsRepository.temperature.collectAsState()
    val currentTokens by adminViewModel.settingsRepository.maxTokens.collectAsState()
    val currentSysPrompt by adminViewModel.settingsRepository.systemPrompt.collectAsState()
    val currentStreaming by adminViewModel.settingsRepository.streamingEnabled.collectAsState()

    var tempValue by remember(currentTemp) { mutableStateOf(currentTemp) }
    var tokensValue by remember(currentTokens) { mutableStateOf(currentTokens.toFloat()) }
    var promptInput by remember(currentSysPrompt) { mutableStateOf(currentSysPrompt) }
    var streamingState by remember(currentStreaming) { mutableStateOf(currentStreaming) }

    var saveSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        adminViewModel.saveApiParameters(
                            temp = (tempValue * 10).roundToInt() / 10f,
                            maxTokens = tokensValue.toInt(),
                            sysPrompt = promptInput.trim(),
                            streaming = streamingState
                        )
                        saveSuccess = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .testTag("save_app_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NovaButtonGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Settings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
                .background(DarkBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Customize your AI parameters",
                fontSize = 14.sp,
                color = DarkTextSecondary
            )

            if (saveSuccess) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "App settings updated successfully!",
                        color = StatusGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Temperature Slider Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Temperature", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NovaBlue.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", tempValue),
                                color = NovaBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Slider(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        valueRange = 0.0f..1.5f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = NovaBlue,
                            activeTrackColor = NovaBlue,
                            inactiveTrackColor = DarkBorder
                        )
                    )

                    Text(
                        text = "Higher values make output more creative and diverse, lower values make it more focused and deterministic.",
                        fontSize = 12.sp,
                        color = DarkTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Max Tokens Slider Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Max Tokens", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NovaPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = tokensValue.toInt().toString(),
                                color = NovaPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Slider(
                        value = tokensValue,
                        onValueChange = { tokensValue = it },
                        valueRange = 256f..4096f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = NovaPurple,
                            activeTrackColor = NovaPurple,
                            inactiveTrackColor = DarkBorder
                        )
                    )

                    Text(
                        text = "Maximum number of tokens to generate in response. 1,000 tokens is approximately 750 words.",
                        fontSize = 12.sp,
                        color = DarkTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Streaming Toggle Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Streaming Responses", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Stream responses token by token with typing cursor", fontSize = 12.sp, color = DarkTextSecondary)
                    }

                    Switch(
                        checked = streamingState,
                        onCheckedChange = { streamingState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NovaBlue
                        )
                    )
                }
            }

            // System Prompt Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("System Prompt", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Default system instructions given to the AI across conversations.", fontSize = 12.sp, color = DarkTextSecondary)

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_system_prompt_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

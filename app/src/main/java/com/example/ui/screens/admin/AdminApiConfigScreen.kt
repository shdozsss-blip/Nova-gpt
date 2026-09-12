package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApiConfigScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentApiKey by adminViewModel.settingsRepository.openRouterApiKey.collectAsState()
    val apiStatus by adminViewModel.settingsRepository.apiStatus.collectAsState()
    val isApiEnabled by adminViewModel.settingsRepository.isApiEnabled.collectAsState()

    val isTestingApi by adminViewModel.isTestingApi.collectAsState()
    val testApiResult by adminViewModel.testApiResult.collectAsState()
    val testApiSuccess by adminViewModel.testApiSuccess.collectAsState()

    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var modelIdInput by remember { mutableStateOf("openai/gpt-4o") }
    var saveSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Configuration", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Set your OpenRouter API key and model",
                fontSize = 13.sp,
                color = DarkTextSecondary
            )

            // API Master Toggle
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
                        Text("Enable AI Services", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(
                            if (isApiEnabled) "Live requests active" else "Responses will show offline/notice",
                            fontSize = 12.sp,
                            color = DarkTextSecondary
                        )
                    }
                    Switch(
                        checked = isApiEnabled,
                        onCheckedChange = { adminViewModel.setApiEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NovaBlue
                        )
                    )
                }
            }

            // Input Fields Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // API Key Field
                    Text("OpenRouter API Key", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkTextSecondary)
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("sk-or-v1-...", color = DarkTextSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openrouter_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        )
                    )

                    // Model ID Field
                    Text("Model ID", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkTextSecondary)
                    OutlinedTextField(
                        value = modelIdInput,
                        onValueChange = { modelIdInput = it },
                        placeholder = { Text("openai/gpt-4o", color = DarkTextSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_id_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons: Save & Test
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                adminViewModel.saveApiKey(apiKeyInput.trim())
                                saveSuccessMessage = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_api_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NovaBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Configuration", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                adminViewModel.testApiConnection(apiKeyInput.trim(), modelIdInput.trim())
                            },
                            enabled = !isTestingApi,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("test_api_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaPurple),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NovaPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isTestingApi) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NovaPurple)
                            } else {
                                Text("Test Connection", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (saveSuccessMessage) {
                        Text(
                            text = "Configuration saved successfully!",
                            color = StatusGreen,
                            fontSize = 12.sp
                        )
                    }

                    // Test Result Feedback
                    testApiResult?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (testApiSuccess == true) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (testApiSuccess == true) StatusGreen.copy(alpha = 0.5f) else StatusRed.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = if (testApiSuccess == true) StatusGreen else StatusRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Connection Status Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("API Connection Status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status", fontSize = 13.sp, color = DarkTextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (apiStatus == "Connected") StatusGreen else StatusAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = apiStatus,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (apiStatus == "Connected") StatusGreen else StatusAmber
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Default Model", fontSize = 13.sp, color = DarkTextSecondary)
                        Text(modelIdInput, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Popular Models Quick Selector
            Text("Popular OpenRouter Models", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

            val popular = listOf(
                "openai/gpt-4o" to "GPT-4o (Omni multimodal)",
                "anthropic/claude-3.5-sonnet" to "Claude 3.5 Sonnet (Coding & reasoning)",
                "google/gemini-pro-1.5" to "Gemini 1.5 Pro (Massive context)",
                "meta-llama/llama-3.1-70b-instruct" to "Llama 3.1 70B (Open source fast)",
                "deepseek/deepseek-chat" to "DeepSeek V3 (Coding & reasoning)"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                popular.forEach { (id, desc) ->
                    Surface(
                        onClick = { modelIdInput = id },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (modelIdInput == id) NovaBlue else DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(id, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(desc, fontSize = 11.sp, color = DarkTextSecondary)
                            }
                            if (modelIdInput == id) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = NovaBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

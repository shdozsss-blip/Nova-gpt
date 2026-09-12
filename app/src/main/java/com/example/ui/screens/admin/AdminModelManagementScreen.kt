package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModelManagementScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allModels by adminViewModel.allModels.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var modelToEdit by remember { mutableStateOf<AiModelEntity?>(null) }
    var modelToDelete by remember { mutableStateOf<AiModelEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Management", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Model", tint = NovaBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NovaBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Model")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Manage available AI models in Nova (${allModels.size})",
                    fontSize = 13.sp,
                    color = DarkTextSecondary
                )
            }

            items(allModels, key = { it.id }) { model ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (model.isDefault) NovaBlue.copy(alpha = 0.6f) else DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NovaBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = model.iconLabel.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NovaBlue
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (model.isDefault) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NovaBlue.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Default",
                                                color = NovaBlue,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = model.id,
                                    fontSize = 11.sp,
                                    color = DarkTextSecondary
                                )
                            }

                            Switch(
                                checked = model.isEnabled,
                                onCheckedChange = { adminViewModel.toggleModel(model.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NovaBlue
                                )
                            )
                        }

                        Text(
                            text = model.description,
                            fontSize = 12.sp,
                            color = DarkTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Provider: ${model.provider} • ${if (model.supportsVision) "Vision Supported" else "Text Only"}",
                                fontSize = 11.sp,
                                color = DarkTextSecondary.copy(alpha = 0.8f)
                            )

                            Row {
                                if (!model.isDefault) {
                                    TextButton(onClick = { adminViewModel.setDefaultModel(model.id) }) {
                                        Text("Set Default", fontSize = 11.sp, color = NovaCyan)
                                    }
                                }
                                IconButton(onClick = { modelToDelete = model }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Add Model Dialog
    if (showAddDialog) {
        var newId by remember { mutableStateOf("") }
        var newName by remember { mutableStateOf("") }
        var newProvider by remember { mutableStateOf("OpenRouter") }
        var newDescription by remember { mutableStateOf("") }
        var newSupportsVision by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Model") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Model Name (e.g. Gemini 2.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it },
                        label = { Text("Model ID (e.g. google/gemini-2.0-flash)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProvider,
                        onValueChange = { newProvider = it },
                        label = { Text("Provider (e.g. Google)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = newSupportsVision,
                            onCheckedChange = { newSupportsVision = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Supports Multimodal Vision", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newId.isNotBlank() && newName.isNotBlank()) {
                            adminViewModel.addModel(
                                id = newId.trim(),
                                name = newName.trim(),
                                provider = newProvider.trim(),
                                description = newDescription.trim(),
                                iconLabel = newName.take(2),
                                isEnabled = true,
                                isDefault = false,
                                supportsVision = newSupportsVision
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add Model")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Model Dialog
    modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete Model?") },
            text = { Text("Are you sure you want to remove '${model.name}' (${model.id})?") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteModel(model.id)
                        modelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

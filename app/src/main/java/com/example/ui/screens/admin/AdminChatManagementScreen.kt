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
import com.example.data.local.entity.ConversationEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatManagementScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allConversations by adminViewModel.allConversations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var chatToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    var chatToRename by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val filteredChats = remember(allConversations, searchQuery) {
        if (searchQuery.isBlank()) {
            allConversations
        } else {
            allConversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Management", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "View and manage all conversation threads (${allConversations.size})",
                fontSize = 13.sp,
                color = DarkTextSecondary
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats...", color = DarkTextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_search_chats"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedContainerColor = DarkSurface,
                    focusedContainerColor = DarkSurface
                )
            )

            // Chats List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredChats.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No conversations found", color = DarkTextSecondary)
                        }
                    }
                }

                items(filteredChats, key = { it.id }) { chat ->
                    val dateStr = remember(chat.updatedAt) {
                        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(chat.updatedAt))
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = NovaBlue, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(chat.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Model: ${chat.modelId} • $dateStr", fontSize = 11.sp, color = DarkTextSecondary)
                            }

                            // Edit Title
                            IconButton(onClick = {
                                chatToRename = chat
                                renameInput = chat.title
                            }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Rename", tint = DarkTextSecondary, modifier = Modifier.size(18.dp))
                            }

                            // Delete
                            IconButton(onClick = { chatToDelete = chat }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Danger Zone: Delete All Chats
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StatusRed.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Danger Zone", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                        Text("Permanently erase all chat history", fontSize = 12.sp, color = DarkTextSecondary)
                    }

                    Button(
                        onClick = { showDeleteAllDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete All Chats", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Rename Dialog
    chatToRename?.let { chat ->
        AlertDialog(
            onDismissRequest = { chatToRename = null },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    adminViewModel.renameChat(chat.id, renameInput.trim())
                    chatToRename = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Single Dialog
    chatToDelete?.let { chat ->
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Chat?") },
            text = { Text("Delete conversation '${chat.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteChat(chat.id)
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Chats?") },
            text = { Text("This will permanently remove all conversations and message data from the device database.") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteAllChats()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Yes, Delete All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

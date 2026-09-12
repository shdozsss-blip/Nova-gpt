package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ConversationEntity
import com.example.ui.components.NovaStarLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryDrawer(
    viewModel: ChatViewModel,
    onSelectChat: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allConversations by viewModel.allConversations.collectAsState()
    val currentConvId by viewModel.currentConversationId.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = remember(allConversations, searchQuery) {
        if (searchQuery.isBlank()) {
            allConversations
        } else {
            allConversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    var chatToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    var chatToRename by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameTitleInput by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCloseDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Close Drawer")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Nova",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Chatbot AI",
                            fontSize = 11.sp,
                            color = NovaPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = onNewChatClick) {
                    NovaStarLogo(size = 32.dp, animated = false)
                }
            }

            // Search Bar & + New Chat Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search chats...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("search_chats_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedBorderColor = NovaBlue,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )

                // + New Chat Pill
                Button(
                    onClick = onNewChatClick,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("drawer_new_chat_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NovaBlue),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("+ New Chat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Conversations List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filteredConversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No chats found" else "No chat history yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(filteredConversations, key = { it.id }) { conv ->
                    val isSelected = conv.id == currentConvId
                    val dateFormatted = remember(conv.updatedAt) {
                        formatChatDate(conv.updatedAt)
                    }

                    Surface(
                        onClick = {
                            onSelectChat(conv.id)
                            onCloseDrawer()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, NovaBlue.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chat icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForTitle(conv.title),
                                    contentDescription = null,
                                    tint = if (isSelected) NovaBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conv.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = dateFormatted,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Model: ${conv.modelId.substringAfter('/')}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }

                            // Delete Action
                            IconButton(
                                onClick = { chatToDelete = conv },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    chatToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Chat?") },
            text = { Text("Are you sure you want to delete '${conv.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(conv.id)
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatChatDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        diff < 172800000 -> "Yesterday"
        else -> {
            val days = diff / 86400000
            if (days < 7) "${days}d ago" else SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

private fun getIconForTitle(title: String): androidx.compose.ui.graphics.vector.ImageVector {
    val t = title.lowercase()
    return when {
        t.contains("code") || t.contains("python") || t.contains("dev") -> Icons.Outlined.Code
        t.contains("study") || t.contains("learn") || t.contains("book") -> Icons.Outlined.School
        t.contains("image") || t.contains("photo") -> Icons.Outlined.Image
        t.contains("math") -> Icons.Outlined.Calculate
        t.contains("idea") || t.contains("plan") -> Icons.Outlined.Lightbulb
        else -> Icons.Outlined.ChatBubbleOutline
    }
}

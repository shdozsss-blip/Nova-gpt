package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.MessageEntity
import com.example.ui.theme.*

@Composable
fun MessageBubble(
    message: MessageEntity,
    isStreamingThisMessage: Boolean = false,
    fontSize: TextUnit = 15.sp,
    onLike: (Boolean?) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onContinue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.role == "user"

    if (isUser) {
        // User message bubble (Aligned Right)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // Attached image if present
                if (!message.imageBase64.isNullOrBlank()) {
                    val bitmap = remember(message.imageBase64) {
                        try {
                            val decoded = Base64.decode(message.imageBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Uploaded image",
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .size(140.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (!message.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(message.imageUri),
                        contentDescription = "Uploaded image",
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Bubble container
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 4.dp
                            )
                        )
                        .background(
                            brush = NovaButtonGradient
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.4f,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } else {
        // Assistant Message (Aligned Left with Avatar)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 36.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            NovaAvatar(
                size = 32.dp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Assistant Message Content
                MarkdownView(
                    content = if (isStreamingThisMessage) "${message.content} ▌" else message.content,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    baseFontSize = fontSize
                )

                // Message Action Buttons (Appear when response is not actively streaming)
                if (!isStreamingThisMessage && message.content.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy Button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Nova Message", message.content))
                            },
                            modifier = Modifier.size(32.dp).testTag("action_copy")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy message",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Thumbs Up / Like
                        IconButton(
                            onClick = {
                                onLike(if (message.isLiked == true) null else true)
                            },
                            modifier = Modifier.size(32.dp).testTag("action_like")
                        ) {
                            Icon(
                                imageVector = if (message.isLiked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (message.isLiked == true) NovaBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Thumbs Down / Dislike
                        IconButton(
                            onClick = {
                                onLike(if (message.isLiked == false) null else false)
                            },
                            modifier = Modifier.size(32.dp).testTag("action_dislike")
                        ) {
                            Icon(
                                imageVector = if (message.isLiked == false) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (message.isLiked == false) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Regenerate Button
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(32.dp).testTag("action_regenerate")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Regenerate response",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Share Button
                        IconButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, message.content)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Nova AI Response")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.size(32.dp).testTag("action_share")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

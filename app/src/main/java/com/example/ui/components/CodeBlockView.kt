package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CodeBlockView(
    code: String,
    language: String = "code",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, CodeBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CodeBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeHeader)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" }.lowercase(),
                    color = DarkTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Copy button
                FilledTonalButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Nova Code", code.trim())
                        clipboard.setPrimaryClip(clip)
                        copied = true
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("copy_code_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (copied) StatusGreen.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        contentColor = if (copied) StatusGreen else DarkTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    AnimatedContent(
                        targetState = copied,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "copy_state"
                    ) { isCopied ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = if (isCopied) "Copied" else "Copy Code",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isCopied) "Copied!" else "Copy",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = CodeBorder)

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = code.trim(),
                    color = CodeText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

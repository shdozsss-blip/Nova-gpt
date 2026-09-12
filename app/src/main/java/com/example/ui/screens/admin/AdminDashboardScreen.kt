package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NovaStarLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    onNavigateToApiConfig: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToFeatures: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToAppSettings: () -> Unit,
    onLogout: () -> Unit,
    onBackToApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalChats by adminViewModel.totalChats.collectAsState()
    val apiStatus by adminViewModel.settingsRepository.apiStatus.collectAsState()
    val allModels by adminViewModel.allModels.collectAsState()
    val defaultModel = remember(allModels) { allModels.firstOrNull { it.isDefault }?.id ?: "openai/gpt-4o" }
    val logs by adminViewModel.adminLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NovaStarLogo(size = 28.dp, animated = false)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Nova Admin", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToApp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to App")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        adminViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = StatusRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logout", color = StatusRed, fontSize = 13.sp)
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
            // Welcome Header
            Column {
                Text(
                    text = "Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Welcome, Shahniar",
                    fontSize = 16.sp,
                    color = NovaBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Here's what's happening with your app.",
                    fontSize = 13.sp,
                    color = DarkTextSecondary
                )
            }

            // Metric Cards Grid (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Chats",
                    value = totalChats.toString(),
                    subtext = "+12% activity",
                    icon = Icons.Outlined.Chat,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Users",
                    value = "1",
                    subtext = "Local Client",
                    icon = Icons.Outlined.People,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val statusColor = when (apiStatus) {
                    "Connected" -> StatusGreen
                    "Disabled" -> StatusRed
                    else -> StatusAmber
                }
                MetricCard(
                    title = "API Status",
                    value = apiStatus,
                    subtext = "OpenRouter",
                    icon = Icons.Outlined.CloudQueue,
                    valueColor = statusColor,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Current Model",
                    value = defaultModel.substringAfter('/'),
                    subtext = defaultModel,
                    icon = Icons.Outlined.Psychology,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Actions Section
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "Configure API",
                        description = "Set your OpenRouter key",
                        icon = Icons.Outlined.VpnKey,
                        onClick = onNavigateToApiConfig,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Manage Models",
                        description = "Add/edit AI models",
                        icon = Icons.Outlined.Tune,
                        onClick = onNavigateToModels,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "Manage Chats",
                        description = "View all conversations",
                        icon = Icons.Outlined.Forum,
                        onClick = onNavigateToChats,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Feature Controls",
                        description = "Enable/disable tools",
                        icon = Icons.Outlined.ToggleOn,
                        onClick = onNavigateToFeatures,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "View Logs",
                        description = "Check app activity audit",
                        icon = Icons.Outlined.Assignment,
                        onClick = onNavigateToLogs,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "App Settings",
                        description = "Customize your parameters",
                        icon = Icons.Outlined.Settings,
                        onClick = onNavigateToAppSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Activity Section
            Text(
                text = "Recent Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val displayLogs = logs.take(6)
                    if (displayLogs.isEmpty()) {
                        Text(
                            text = "No recent activity recorded yet.",
                            fontSize = 13.sp,
                            color = DarkTextSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        displayLogs.forEach { logItem ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = NovaBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = logItem.action,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = logItem.details,
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = formatTimestamp(logItem.timestamp),
                                    fontSize = 11.sp,
                                    color = DarkTextSecondary
                                )
                            }
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = DarkTextSecondary)
                Icon(icon, contentDescription = null, tint = NovaPurple, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(subtext, fontSize = 11.sp, color = DarkTextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
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
                    .background(NovaBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NovaBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(description, fontSize = 11.sp, color = DarkTextSecondary)
            }
        }
    }
}

private fun formatTimestamp(time: Long): String {
    val diff = System.currentTimeMillis() - time
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
    }
}

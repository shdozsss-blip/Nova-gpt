package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.admin.*
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.explore.ExploreScreen
import com.example.ui.screens.history.ChatHistoryDrawer
import com.example.ui.screens.newchat.NewChatModal
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.welcome.WelcomeScreen
import com.example.ui.theme.NovaAiTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val adminViewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by chatViewModel.settingsRepository.themeMode.collectAsState()
            val isDarkTheme = themeMode == "dark"

            NovaAiTheme(darkTheme = isDarkTheme) {
                NovaApp(
                    chatViewModel = chatViewModel,
                    adminViewModel = adminViewModel
                )
            }
        }
    }
}

@Composable
fun NovaApp(
    chatViewModel: ChatViewModel,
    adminViewModel: AdminViewModel
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "welcome",
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Welcome Splash
        composable(
            route = "welcome",
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) }
        ) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate("chat") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // 2. Main Chat (with Chat History Drawer)
        composable(
            route = "chat",
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    ChatHistoryDrawer(
                        viewModel = chatViewModel,
                        onSelectChat = { convId ->
                            chatViewModel.selectConversation(convId)
                        },
                        onNewChatClick = {
                            coroutineScope.launch { drawerState.close() }
                            navController.navigate("new_chat")
                        },
                        onCloseDrawer = {
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                ChatScreen(
                    viewModel = chatViewModel,
                    onOpenDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onOpenNewChat = {
                        navController.navigate("new_chat")
                    },
                    onOpenSettings = {
                        navController.navigate("settings")
                    }
                )
            }
        }

        // 3. New Chat Configuration Modal
        composable(
            route = "new_chat",
            enterTransition = { slideInVertically(tween(350)) { it } },
            exitTransition = { slideOutVertically(tween(350)) { it } }
        ) {
            NewChatModal(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onStartChat = {
                    navController.popBackStack()
                }
            )
        }

        // 4. Settings Screen
        composable(
            route = "settings",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            SettingsScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAdmin = {
                    navController.navigate("admin_login")
                },
                onNavigateToHistory = {
                    navController.navigate("chat")
                    coroutineScope.launch { drawerState.open() }
                }
            )
        }

        // 5. Explore Screen
        composable(
            route = "explore",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            ExploreScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onSelectPrompt = { prompt ->
                    chatViewModel.startNewChat()
                    chatViewModel.inputText.value = prompt
                    navController.navigate("chat") {
                        popUpTo("chat") { inclusive = true }
                    }
                }
            )
        }

        // 6. Admin Login
        composable(
            route = "admin_login",
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            AdminLoginScreen(
                adminViewModel = adminViewModel,
                onLoginSuccess = {
                    navController.navigate("admin_dashboard") {
                        popUpTo("admin_login") { inclusive = true }
                    }
                },
                onBackToApp = {
                    navController.popBackStack()
                }
            )
        }

        // 7. Admin Dashboard
        composable(
            route = "admin_dashboard",
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            AdminDashboardScreen(
                adminViewModel = adminViewModel,
                onNavigateToApiConfig = { navController.navigate("admin_api") },
                onNavigateToModels = { navController.navigate("admin_models") },
                onNavigateToChats = { navController.navigate("admin_chats") },
                onNavigateToFeatures = { navController.navigate("admin_features") },
                onNavigateToLogs = { navController.navigate("admin_logs") },
                onNavigateToAppSettings = { navController.navigate("admin_app_settings") },
                onLogout = {
                    navController.navigate("admin_login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                },
                onBackToApp = {
                    navController.navigate("chat") {
                        popUpTo("chat") { inclusive = true }
                    }
                }
            )
        }

        // 8. Admin API Config
        composable(
            route = "admin_api",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            AdminApiConfigScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 9. Admin Model Management
        composable(
            route = "admin_models",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            AdminModelManagementScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 10. Admin Chat Management
        composable(
            route = "admin_chats",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            AdminChatManagementScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 11. Admin Feature Controls
        composable(
            route = "admin_features",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            AdminFeatureControlsScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 12. Admin Activity Logs
        composable(
            route = "admin_logs",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            AdminLogsScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 13. Admin App Settings
        composable(
            route = "admin_app_settings",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            AdminAppSettingsScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

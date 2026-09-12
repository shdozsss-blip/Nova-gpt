package com.example.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.viewmodel.ChatViewModel

data class PromptCategory(
    val title: String,
    val icon: ImageVector,
    val prompts: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        PromptCategory(
            title = "Coding & Engineering",
            icon = Icons.Outlined.Code,
            prompts = listOf(
                "Write a Python script to scrape website headers asynchronously",
                "How do coroutines and StateFlow work in modern Jetpack Compose?",
                "Debug this SQL query: SELECT * FROM users WHERE active = 1 GROUP BY id",
                "Explain the Clean Architecture pattern in mobile development"
            )
        ),
        PromptCategory(
            title = "Writing & Ideation",
            icon = Icons.Outlined.EditNote,
            prompts = listOf(
                "Draft a professional announcement for a mobile app launch",
                "Give me 5 catchy taglines for an AI productivity app",
                "Write an executive summary for a seed stage pitch deck",
                "Draft an email to negotiate project timelines politely"
            )
        ),
        PromptCategory(
            title = "Science & Math",
            icon = Icons.Outlined.Calculate,
            prompts = listOf(
                "Explain the Heisenberg Uncertainty Principle in plain English",
                "Solve this step-by-step: Solve 3x^2 - 12x + 9 = 0",
                "How do transformer neural networks process token attention?",
                "What is the mathematical definition of entropy in thermodynamics?"
            )
        ),
        PromptCategory(
            title = "Study & Productivity",
            icon = Icons.Outlined.School,
            prompts = listOf(
                "Create a 4-week study plan for the AWS Solutions Architect exam",
                "Explain how spaced repetition and flashcards optimize memory",
                "Give me a 5-minute morning meditation and breathing script",
                "Summarize the key tenets of the Pareto 80/20 principle"
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore Prompts", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Tap any suggestion to start a conversation with Nova AI",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(categories) { category ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NovaBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = NovaBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        category.prompts.forEach { prompt ->
                            Surface(
                                onClick = { onSelectPrompt(prompt) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prompt,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowOutward,
                                        contentDescription = "Use prompt",
                                        tint = NovaBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

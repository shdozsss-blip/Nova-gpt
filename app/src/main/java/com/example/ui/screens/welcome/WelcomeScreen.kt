package com.example.ui.screens.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.NovaStarLogo
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Cosmic background
        Image(
            painter = painterResource(id = R.drawable.bg_nova_welcome),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        // Gradient overlay for smooth dark atmosphere
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkBg.copy(alpha = 0.5f),
                            DarkBg
                        )
                    )
                )
        )

        // Centered Brand Content
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + slideInVertically(tween(800), initialOffsetY = { 60 }),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                NovaStarLogo(
                    size = 110.dp,
                    animated = true
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Nova",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Chatbot AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NovaPurple,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Your AI companion\nfor a smarter tomorrow.",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = DarkTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Get Started Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 40.dp)
                .navigationBarsPadding()
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("get_started_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovaButtonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Arrow",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NovaBlue
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaPurple

@Composable
fun NovaStarLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "star_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val currentScale = if (animated) pulseScale else 1f

    Box(
        modifier = modifier
            .size(size)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width * currentScale
            val h = this.size.height * currentScale
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f

            // Outer soft glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NovaPurple.copy(alpha = 0.45f),
                        NovaBlue.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(cx, cy)
            )

            // 4-pointed radiant star path
            val path = Path().apply {
                val outerRx = w * 0.46f
                val outerRy = h * 0.46f
                val innerR = w * 0.12f

                moveTo(cx, cy - outerRy) // Top tip
                cubicTo(cx, cy - innerR, cx + innerR, cy, cx + outerRx, cy) // Top to right
                cubicTo(cx + innerR, cy, cx, cy + innerR, cx, cy + outerRy) // Right to bottom
                cubicTo(cx, cy + innerR, cx - innerR, cy, cx - outerRx, cy) // Bottom to left
                cubicTo(cx - innerR, cy, cx, cy - innerR, cx, cy - outerRy) // Left to top
                close()
            }

            // Star gradient fill
            val gradientBrush = Brush.linearGradient(
                colors = listOf(NovaCyan, NovaBlue, NovaPurple),
                start = Offset(cx - w * 0.35f, cy - h * 0.35f),
                end = Offset(cx + w * 0.35f, cy + h * 0.35f)
            )

            drawPath(path = path, brush = gradientBrush)

            // Center radiant highlight core
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = w * 0.08f,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
fun NovaAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF161A2E), Color(0xFF0D0F1B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        NovaStarLogo(
            size = size * 0.78f,
            animated = false
        )
    }
}

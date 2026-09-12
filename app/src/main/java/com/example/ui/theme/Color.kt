package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Nova Brand Accents
val NovaBlue = Color(0xFF4E75FF)
val NovaPurple = Color(0xFF8C52FF)
val NovaCyan = Color(0xFF00D2FF)
val NovaIndigo = Color(0xFF6366F1)
val NovaViolet = Color(0xFF7C3AED)

// Gradients
val NovaBrandGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4E75FF), Color(0xFF8C52FF), Color(0xFF00D2FF))
)

val NovaButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF4E75FF), Color(0xFF8C52FF))
)

val NovaCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF191D33), Color(0xFF101324))
)

// Dark Theme Palette
val DarkBg = Color(0xFF090B14)
val DarkSurface = Color(0xFF121625)
val DarkSurfaceVariant = Color(0xFF1A1F33)
val DarkBorder = Color(0xFF252B44)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// Light Theme Palette
val LightBg = Color(0xFFF6F8FD)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF2F9)
val LightBorder = Color(0xFFE0E5F2)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)

// Status & Indicators
val StatusGreen = Color(0xFF10B981)
val StatusAmber = Color(0xFFF59E0B)
val StatusRed = Color(0xFFEF4444)

// Code Block Dark Canvas
val CodeBackground = Color(0xFF0C0E1A)
val CodeBorder = Color(0xFF22273D)
val CodeHeader = Color(0xFF15192C)
val CodeText = Color(0xFFE2E8F0)

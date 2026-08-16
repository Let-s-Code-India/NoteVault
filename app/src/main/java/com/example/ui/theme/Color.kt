package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Branding & Accents
val IndigoPrimary = Color(0xFF6366F1)
val IndigoSecondary = Color(0xFF818CF8)
val IndigoDark = Color(0xFF4F46E5)
val CyanAccent = Color(0xFF06B6D4)
val EmeraldSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val RoseDanger = Color(0xFFF43F5E)
val PurpleAccent = Color(0xFF8B5CF6)
val BlueAccent = Color(0xFF3B82F6)

// Dark Theme Surfaces
val SlateBackgroundDark = Color(0xFF0B0F19)
val SlateSurfaceDark = Color(0xFF131B2E)
val SlateSurfaceVariantDark = Color(0xFF1E293B)
val SlateCardDark = Color(0xFF182238)
val SlateBorderDark = Color(0xFF28354E)

// Light Theme Surfaces
val SlateBackgroundLight = Color(0xFFF8FAFC)
val SlateSurfaceLight = Color(0xFFFFFFFF)
val SlateSurfaceVariantLight = Color(0xFFF1F5F9)
val SlateCardLight = Color(0xFFFFFFFF)
val SlateBorderLight = Color(0xFFE2E8F0)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
)
val AccentGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
)
val AmberGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFF97316))
)
val EmeraldGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
)
val CardShimmerGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF6366F1).copy(alpha = 0.08f), Color(0xFF8B5CF6).copy(alpha = 0.02f))
)



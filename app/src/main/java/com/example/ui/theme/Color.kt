package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ThemeState {
    var isDark by mutableStateOf(true)
}

// Modern High-Energy Sporty Interface Color Scheme (Dynamic based on Theme State)
val OrangePrimary: Color
    get() = if (ThemeState.isDark) Color(0xFFFF6A00) else Color(0xFFFF6A00)

val OrangeSecondary: Color
    get() = if (ThemeState.isDark) Color(0xFFFF8533) else Color(0xFFFF8533)

val OrangeTertiary: Color
    get() = if (ThemeState.isDark) Color(0xFFFFC857) else Color(0xFFFFC857)

val SlateDarkBackground: Color
    get() = if (ThemeState.isDark) Color(0xFF08111D) else Color(0xFFF8FAFC)

val SlateCardSurface: Color
    get() = if (ThemeState.isDark) Color(0xFF111827) else Color(0xFFFFFFFF)

val SlateCardSurfaceVariant: Color
    get() = if (ThemeState.isDark) Color(0xFF1F2937) else Color(0xFFF3F4F6)

val SlateTextPrimary: Color
    get() = if (ThemeState.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val SlateTextSecondary: Color
    get() = if (ThemeState.isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

val NeonTealAccent: Color
    get() = if (ThemeState.isDark) Color(0xFF2FA7FF) else Color(0xFF2FA7FF)

val NeonTealMuted: Color
    get() = if (ThemeState.isDark) Color(0xFF0C243C) else Color(0xFFE0F2FE)

val RunColor: Color
    get() = Color(0xFFFF6A00)

val RideColor: Color
    get() = Color(0xFF2FA7FF)

val SwimColor: Color
    get() = Color(0xFF38BDF8)

val HikeColor: Color
    get() = Color(0xFFF59E0B)

val WalkColor: Color
    get() = Color(0xFF10B981)

val PRColor: Color
    get() = Color(0xFFFFC857)             // Gold for Achievement PRs

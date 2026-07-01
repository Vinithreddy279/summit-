package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SummitTheme(content: @Composable () -> Unit) {
    val colorScheme = if (ThemeState.isDark) {
        darkColorScheme(
            primary = OrangePrimary,
            secondary = OrangeSecondary,
            tertiary = NeonTealAccent,
            background = SlateDarkBackground,
            surface = SlateCardSurface,
            surfaceVariant = SlateCardSurfaceVariant,
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onTertiary = Color.Black,
            onBackground = SlateTextPrimary,
            onSurface = SlateTextPrimary,
            onSurfaceVariant = SlateTextSecondary,
            error = Color(0xFFCF6679)
        )
    } else {
        lightColorScheme(
            primary = OrangePrimary,
            secondary = OrangeSecondary,
            tertiary = NeonTealAccent,
            background = SlateDarkBackground,
            surface = SlateCardSurface,
            surfaceVariant = SlateCardSurfaceVariant,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = SlateTextPrimary,
            onSurface = SlateTextPrimary,
            onSurfaceVariant = SlateTextSecondary,
            error = Color(0xFFB3261E)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

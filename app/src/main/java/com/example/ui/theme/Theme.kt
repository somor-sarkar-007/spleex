package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SpleexDarkColorScheme = darkColorScheme(
    primary = SpleexPinkPrimary,
    onPrimary = SpleexOnPinkDark,
    primaryContainer = SpleexPinkContainerDark,
    onPrimaryContainer = SpleexPinkSoft,
    secondary = SpleexVioletAccent,
    onSecondary = Color(0xFF1F123E),
    secondaryContainer = SpleexVioletContainerDark,
    onSecondaryContainer = Color(0xFFE5DCFF),
    tertiary = SpleexPeachAccent,
    onTertiary = Color(0xFF3A081A),
    background = SpleexDarkBackground,
    onBackground = SpleexDarkTextPrimary,
    surface = SpleexDarkSurface,
    onSurface = SpleexDarkTextPrimary,
    surfaceVariant = SpleexDarkSurfaceVariant,
    onSurfaceVariant = SpleexDarkTextSecondary,
    surfaceContainerHigh = SpleexDarkSurfaceElevated,
    outline = SpleexDarkOutline,
    outlineVariant = Color(0xFF231F2F),
    error = SpleexErrorDark,
    onError = Color(0xFF3B0606)
)

private val SpleexLightColorScheme = lightColorScheme(
    primary = SpleexPinkPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = SpleexPinkContainerLight,
    onPrimaryContainer = Color(0xFF4A0720),
    secondary = Color(0xFF6B4EFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE8FF),
    onSecondaryContainer = Color(0xFF21105E),
    tertiary = Color(0xFFE86A92),
    onTertiary = Color.White,
    background = SpleexLightBackground,
    onBackground = SpleexLightTextPrimary,
    surface = SpleexLightSurface,
    onSurface = SpleexLightTextPrimary,
    surfaceVariant = SpleexLightSurfaceVariant,
    onSurfaceVariant = SpleexLightTextSecondary,
    surfaceContainerHigh = Color(0xFFF9F3F6),
    outline = SpleexLightOutline,
    outlineVariant = Color(0xFFF0E6EB),
    error = SpleexErrorLight,
    onError = Color.White
)

val SpleexShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SpleexTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpleexDarkColorScheme else SpleexLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpleexTypography,
        shapes = SpleexShapes,
        content = content
    )
}

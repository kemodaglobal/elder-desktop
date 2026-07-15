package com.elderdesktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.elderdesktop.DesktopSettings

@Composable
fun ElderDesktopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled by default for better theme consistency
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settings = DesktopSettings(context)
    
    val primaryColor = when (settings.themeChoice) {
        "emerald" -> EmeraldGreen
        "rose" -> RoseRed
        "orange" -> WarmOrange
        "high_contrast" -> Color.Black
        else -> ClassicBlue
    }

    val isHighContrast = settings.themeChoice == "high_contrast" || settings.highContrastMode

    val colorScheme = if (isHighContrast) {
        darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            secondary = Color.White,
            onSecondary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color.Black,
            onSurfaceVariant = Color.White,
            outline = Color.White
        )
    } else if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            secondary = primaryColor.copy(alpha = 0.7f),
            tertiary = primaryColor.copy(alpha = 0.5f)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = primaryColor.copy(alpha = 0.7f),
            tertiary = primaryColor.copy(alpha = 0.5f)
        )
    }

    val fontFamily = when (settings.fontChoice) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(fontFamily, isHighContrast),
        content = content
    )
}

package com.elderdesktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
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
    val isHolo = settings.uiStyle == "holo"

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
    } else if (isHolo) {
        darkColorScheme(
            primary = HoloBlue,
            onPrimary = Color.Black,
            secondary = HoloBlue,
            onSecondary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = HoloGray,
            onSurfaceVariant = Color.White,
            outline = HoloBlue
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

    // Adjust shapes for Holo
    val shapes = if (isHolo) {
        androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        )
    } else {
        androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(fontFamily, isHighContrast),
        shapes = shapes,
        content = content
    )
}

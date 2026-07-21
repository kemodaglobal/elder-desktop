package com.elderdesktop.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun getContrastColor(background: Color): Color {
    val luminance = 0.299 * background.red + 0.587 * background.green + 0.114 * background.blue
    return if (luminance > 0.5) Color.Black else Color.White
}

fun getIconShape(shape: String): Shape {
    return when (shape) {
        "circle" -> CircleShape
        "square" -> RectangleShape
        "native" -> RectangleShape
        else -> RoundedCornerShape(16.dp)
    }
}

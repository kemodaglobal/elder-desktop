package com.elderdesktop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun getTypography(fontFamily: FontFamily, isHighContrast: Boolean = false): Typography {
    val weightNormal = if (isHighContrast) FontWeight.Bold else FontWeight.Normal
    val weightMedium = if (isHighContrast) FontWeight.ExtraBold else FontWeight.Medium
    val weightBold = if (isHighContrast) FontWeight.Black else FontWeight.Bold

    return Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = weightNormal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = weightBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = weightMedium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}


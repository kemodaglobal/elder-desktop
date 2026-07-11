package com.elderdesktop.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable,
    val backgroundColor: Color = Color(0xFF1A5F7A)
)

enum class AppType {
    WEATHER, GALLERY, SETTINGS, DIALER, MESSAGING, CONTACTS, CAMERA
}

sealed class GridItem {
    data class App(val info: AppInfo) : GridItem()
    data class SpeedDial(val index: Int) : GridItem()
}

data class DesktopConfig(
    val firstScreenTypes: List<AppType> = emptyList(),
    val secondScreenPackages: List<String> = emptyList()
)
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
    WEATHER, GALLERY, SETTINGS, DIALER, MESSAGING, CONTACTS, CAMERA, BROWSER, APP_STORE
}

sealed class GridItem {
    data class App(val info: AppInfo) : GridItem()
    data class SpeedDial(val index: Int) : GridItem()
}

data class DesktopConfig(
    val firstScreen: List<DesktopXmlItem> = emptyList(),
    val secondScreen: List<DesktopXmlItem> = emptyList()
)

sealed class DesktopXmlItem {
    data class Type(val type: AppType) : DesktopXmlItem()
    data class Package(val packageName: String) : DesktopXmlItem()
}

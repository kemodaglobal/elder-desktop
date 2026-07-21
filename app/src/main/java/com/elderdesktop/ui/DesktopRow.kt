package com.elderdesktop.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.elderdesktop.DesktopSettings
import com.elderdesktop.SettingsActivity
import com.elderdesktop.WeatherActivity
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.AppType
import com.elderdesktop.model.GridItem
import com.elderdesktop.notification.NotificationRepository
import com.elderdesktop.util.AppUtils

@Composable
fun DesktopRow(
    rowItems: List<GridItem?>,
    rowIndex: Int,
    colCount: Int,
    modifier: Modifier = Modifier,
    labelSize: TextUnit,
    highContrastFilter: ColorFilter?,
    settings: DesktopSettings,
    firstScreenMap: Map<AppType, List<String>>,
    weatherText: String,
    isWeatherAlert: Boolean,
    locationCity: String,
    currentTemperature: String,
    weatherCode: Int,
    onAppLaunch: (AppInfo) -> Unit,
    onSpeak: (String) -> Unit,
    showUnlockDialog: () -> Unit,
    setIsAddingWeather: (Boolean) -> Unit,
    setPendingApp: (AppInfo?) -> Unit,
    setPendingSpeedDialIndex: (Int) -> Unit,
    setEditingSpeedDialIndex: (Int) -> Unit,
    showAddContactDialog: () -> Unit,
    showLocationSelectionDialog: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (rowItems.firstOrNull() is GridItem.ClockWidget) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (!settings.isBasicMode) {
                    ClockWidget(
                        weatherText = weatherText, isWeatherAlert = isWeatherAlert,
                        locationCity = locationCity, currentTemperature = currentTemperature,
                        weatherCode = weatherCode,
                        modifier = Modifier.fillMaxSize(),
                        fontSizeMultiplier = settings.fontSizeMultiplier,
                        onClick = { context.startActivity(Intent(context, WeatherActivity::class.java)) },
                        onAddLocation = {
                            if (settings.usePasscode) {
                                setIsAddingWeather(true)
                                showUnlockDialog()
                            } else {
                                showLocationSelectionDialog()
                            }
                        }
                    )
                } else {
                    SimpleClockWidget(
                        modifier = Modifier.fillMaxSize(),
                        fontSizeMultiplier = settings.fontSizeMultiplier,
                        weatherCode = weatherCode
                    )
                }
            }
        } else {
            rowItems.forEachIndexed { colIndex, item ->
                if (item != null) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (settings.enableDesktopNotifications && !settings.enableFloatingNotifications && item is GridItem.SpeedDial && item.index == -2) {
                            val notifications = NotificationRepository.activeNotifications
                            val itemIndex = rowIndex * colCount + colIndex
                            if (itemIndex < notifications.size) {
                                NotificationItem(notifications[itemIndex])
                            }
                        } else {
                            when (item) {
                                is GridItem.App -> {
                                    DesktopItem(
                                        app = item.info, modifier = Modifier.fillMaxSize(),
                                        labelSize = labelSize, iconSizeMultiplier = settings.iconSizeMultiplier,
                                        iconShape = settings.iconShape, colorFilter = highContrastFilter
                                    ) {
                                        val isSettingsApp = firstScreenMap[AppType.SETTINGS]?.contains(item.info.packageName) == true
                                        val isCameraApp = firstScreenMap[AppType.CAMERA]?.contains(item.info.packageName) == true
                                        val isDialerApp = firstScreenMap[AppType.DIALER]?.contains(item.info.packageName) == true
                                        val isContactsApp = firstScreenMap[AppType.CONTACTS]?.contains(item.info.packageName) == true

                                        if (isSettingsApp) {
                                            if (settings.usePasscode) {
                                                setPendingApp(item.info)
                                                showUnlockDialog()
                                            } else {
                                                onAppLaunch(item.info)
                                                context.startActivity(Intent(context, SettingsActivity::class.java))
                                            }
                                        } else if (item.info.packageName == context.packageName && firstScreenMap[AppType.WEATHER]?.contains(item.info.packageName) == true) {
                                            onAppLaunch(item.info)
                                            context.startActivity(Intent(context, WeatherActivity::class.java))
                                        } else if (isCameraApp) {
                                            onAppLaunch(item.info)
                                            AppUtils.launchCamera(context)
                                        } else if (isDialerApp) {
                                            onAppLaunch(item.info)
                                            AppUtils.launchDialer(context)
                                        } else if (isContactsApp) {
                                            onAppLaunch(item.info)
                                            AppUtils.launchContacts(context)
                                        } else {
                                            onAppLaunch(item.info)
                                            AppUtils.launchApp(context, item.info)
                                        }
                                    }
                                }

                                is GridItem.SpeedDial -> {
                                    SpeedDialItem(
                                        index = item.index,
                                        settings = settings,
                                        modifier = Modifier.fillMaxSize(),
                                        labelSize = labelSize,
                                        iconSizeMultiplier = settings.iconSizeMultiplier,
                                        iconShape = settings.iconShape,
                                        colorFilter = highContrastFilter
                                    ) {
                                        val contact = settings.getSpeedDial(item.index)
                                        if (contact == null) {
                                            if (settings.usePasscode) {
                                                setPendingSpeedDialIndex(item.index)
                                                showUnlockDialog()
                                            } else {
                                                setEditingSpeedDialIndex(item.index)
                                                showAddContactDialog()
                                            }
                                        } else {
                                            onSpeak(contact.first)
                                            val dialIntent = Intent(Intent.ACTION_CALL, "tel:${contact.second}".toUri())
                                            try {
                                                context.startActivity(dialIntent)
                                            } catch (_: Exception) {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${contact.second}".toUri()))
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

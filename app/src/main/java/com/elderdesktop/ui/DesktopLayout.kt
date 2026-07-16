package com.elderdesktop.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.SettingsActivity
import com.elderdesktop.WeatherActivity
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.AppType
import com.elderdesktop.model.DesktopConfig
import com.elderdesktop.model.DesktopXmlItem
import com.elderdesktop.model.GridItem
import com.elderdesktop.notification.NotificationRepository
import com.elderdesktop.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

@SuppressLint("SuspiciousIndentation", "LocalContextGetResourceValueCall",
    "ConfigurationScreenWidthHeight"
)
@Composable
fun DesktopLayout(
    onAppLaunch: (AppInfo) -> Unit,
    onSpeak: (String) -> Unit,
    onVoiceAssistant: () -> Unit,
    weatherText: String,
    isWeatherAlert: Boolean,
    locationCity: String = "",
    currentTemperature: String = "",
    weatherCode: Int = 800,
    triggerSettingsUnlock: Boolean = false,
    onSettingsUnlockHandled: () -> Unit = {},
    onRefreshWeather: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val insets = WindowInsets.systemBars.asPaddingValues()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var settingsVersion by remember { mutableIntStateOf(0) }
    val settings = remember(settingsVersion) { DesktopSettings(context) }

    val isHighContrast = settings.themeChoice == "high_contrast" || settings.highContrastMode

    val highContrastFilter = if (isHighContrast) {
        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(
                    2f, 0f, 0f, 0f, -100f,
                    0f, 2f, 0f, 0f, -100f,
                    0f, 0f, 2f, 0f, -100f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    } else null

    val labelSize = 22.sp * settings.fontSizeMultiplier

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLargeScreen = configuration.smallestScreenWidthDp >= 600

    val (rowCount, colCount) = remember(settingsVersion, configuration.screenWidthDp, configuration.screenHeightDp) {
        if (settings.useAutoLayout) {
            if (isLargeScreen) {
                if (configuration.screenWidthDp > configuration.screenHeightDp) (4 to 6) else (5 to 4)
            } else {
                if (configuration.screenHeightDp > 800) (5 to 3) else (4 to 2)
            }
        } else {
            (settings.layoutRows to settings.layoutCols)
        }
    }

    val config = remember(rowCount, colCount) {
        val currentCountry = Locale.getDefault().country
        val xmlId = if (colCount <= 2) {
            when (currentCountry) {
                "CN" -> R.xml.desktop_2x3_china
                "KP" -> R.xml.desktop_2x3_north_korea
                else -> R.xml.desktop_2x3_global
            }
        } else if (colCount == 3) {
            when (currentCountry) {
                "CN" -> R.xml.desktop_3x4_china
                "KP" -> R.xml.desktop_3x4_north_korea
                else -> R.xml.desktop_3x4_global
            }
        } else {
            // Large screen layouts
            R.xml.desktop_3x4_global // Defaulting to 3x4 config for now as 4x3/6x4 specific XMLs don't exist yet, but we'll use the grid logic
        }
        parseDesktopConfig(context, xmlId)
    }

    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showLocationSelectionDialog by remember { mutableStateOf(false) }
    var isAddingWeather by remember { mutableStateOf(false) }
    var editingSpeedDialIndex by remember { mutableIntStateOf(-1) }
    var pendingSpeedDialIndex by remember { mutableIntStateOf(-1) }
    var showPrivacyAgreement by remember { mutableStateOf(!settings.privacyAccepted) }

    if (showPrivacyAgreement) {
        PrivacyAgreementDialog(
            settings = settings,
            onAccept = { showPrivacyAgreement = false },
            onDecline = { (context as? ComponentActivity)?.finish() }
        )
    }

    LaunchedEffect(triggerSettingsUnlock) {
        if (triggerSettingsUnlock) {
            showUnlockDialog = true
            onSettingsUnlockHandled()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                settingsVersion++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var allApps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(settingsVersion) {
        val fetchedApps = withContext(Dispatchers.IO) {
            val allLaunchable = AppUtils.getLaunchableApps(context)
            val firstScreenMap = AppUtils.getFirstScreenPackageMap()
            val neededPackageNames = mutableSetOf<String>()

            if (settings.isBasicMode) {
                listOf(AppType.GALLERY, AppType.CAMERA, AppType.DIALER, AppType.CONTACTS, AppType.SETTINGS).forEach { type ->
                    neededPackageNames.addAll(firstScreenMap[type] ?: emptyList())
                }
            } else {
                config.firstScreen.forEach { item ->
                    when(item) {
                        is DesktopXmlItem.Type -> neededPackageNames.addAll(firstScreenMap[item.type] ?: emptyList())
                        is DesktopXmlItem.Package -> neededPackageNames.add(item.packageName)
                    }
                }
                config.secondScreen.forEach { item ->
                    when(item) {
                        is DesktopXmlItem.Type -> neededPackageNames.addAll(firstScreenMap[item.type] ?: emptyList())
                        is DesktopXmlItem.Package -> neededPackageNames.add(item.packageName)
                    }
                }
                neededPackageNames.addAll(settings.selectedApps)
            }

            allLaunchable.filter { it.packageName in neededPackageNames || (neededPackageNames.isEmpty() && !settings.isBasicMode) }.map { app ->
                val bitmap = app.icon.toBitmap()
                val palette = Palette.from(bitmap).generate()
                val dominantColor = palette.getDominantColor(Color(0xFF1A5F7A).toArgb())
                app.copy(backgroundColor = Color(dominantColor))
            }
        }
        allApps = fetchedApps
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        val firstScreenMap = AppUtils.getFirstScreenPackageMap()
        
        fun resolveXmlItems(items: List<DesktopXmlItem>, usedPackages: MutableSet<String>): List<AppInfo> {
            val resolved = mutableListOf<AppInfo>()
            for (item in items) {
                when (item) {
                    is DesktopXmlItem.Type -> {
                        val possiblePackages = firstScreenMap[item.type] ?: emptyList()
                        val foundApp = allApps.find { it.packageName in possiblePackages && it.packageName !in usedPackages }
                        if (foundApp != null) {
                            resolved.add(foundApp)
                            usedPackages.add(foundApp.packageName)
                        }
                    }
                    is DesktopXmlItem.Package -> {
                        val foundApp = allApps.find { it.packageName == item.packageName && it.packageName !in usedPackages }
                        if (foundApp != null) {
                            resolved.add(foundApp)
                            usedPackages.add(foundApp.packageName)
                        }
                    }
                }
            }
            return resolved
        }

        val usedPackageNames = mutableSetOf<String>()
        val firstScreenFoundApps = resolveXmlItems(config.firstScreen, usedPackageNames)
        val secondScreenApps = resolveXmlItems(config.secondScreen, usedPackageNames)
        val userSelectedApps = allApps.filter { it.packageName in settings.selectedApps && it.packageName !in usedPackageNames }

        val finalPages = mutableListOf<List<GridItem>>()
        val effectiveRows = rowCount - 1
        val pageSize = effectiveRows * colCount

        // Page 0: Notifications (Marker: SpeedDial with index -2)
        if (settings.enableDesktopNotifications) {
            finalPages.add((0 until pageSize).map { GridItem.SpeedDial(-2) })
        }

        // Page 1: Contacts
        finalPages.add((0 until pageSize).map { GridItem.SpeedDial(it) })

        // Page 2: Core Apps
        val appsOnFirstPage = (effectiveRows - 1) * colCount
        finalPages.add(firstScreenFoundApps.take(appsOnFirstPage).map { GridItem.App(it) })

        // Remaining Apps
        val designatedApps = mutableListOf<GridItem>()
        if (firstScreenFoundApps.size > appsOnFirstPage) {
            designatedApps.addAll(firstScreenFoundApps.drop(appsOnFirstPage).map { GridItem.App(it) })
        }
        designatedApps.addAll(secondScreenApps.map { GridItem.App(it) })
        designatedApps.addAll(userSelectedApps.map { GridItem.App(it) })
        if (designatedApps.isNotEmpty()) {
            finalPages.addAll(designatedApps.chunked(pageSize))
        }

        val pagerState = rememberPagerState(
            initialPage = if (settings.enableDesktopNotifications) 2 else 1,
            pageCount = { finalPages.size }
        )
        val coroutineScope = rememberCoroutineScope()

        BackHandler {
            val target = if (settings.enableDesktopNotifications) 2 else 1
            if (pagerState.currentPage != target) {
                coroutineScope.launch { pagerState.animateScrollToPage(target) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isHighContrast) androidx.compose.material3.MaterialTheme.colorScheme.background else Color.Transparent)
                .padding(insets)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                    val pageItems = finalPages[pageIndex]
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Top) {
                        for (rowIndex in 0 until effectiveRows) {
                            Row(
                                modifier = Modifier.weight(1f).padding(bottom = if (rowIndex < effectiveRows - 1) 16.dp else 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Special case for Widget row on Core Apps page
                                val coreAppsPageIndex = if (settings.enableDesktopNotifications) 2 else 1
                                if (pageIndex == coreAppsPageIndex && rowIndex == 0) {
                                    if (!settings.isBasicMode) {
                                        ClockWidget(
                                            weatherText = weatherText, isWeatherAlert = isWeatherAlert,
                                            locationCity = locationCity, currentTemperature = currentTemperature,
                                            weatherCode = weatherCode,
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                            fontSizeMultiplier = settings.fontSizeMultiplier,
                                            onClick = { context.startActivity(Intent(context, WeatherActivity::class.java)) },
                                            onAddLocation = {
                                                if (settings.usePasscode) { isAddingWeather = true; showUnlockDialog = true }
                                                else { showLocationSelectionDialog = true }
                                            }
                                        )
                                    } else {
                                        SimpleClockWidget(
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                            fontSizeMultiplier = settings.fontSizeMultiplier,
                                            weatherCode = weatherCode
                                        )
                                    }
                                } else {
                                    for (colIndex in 0 until colCount) {
                                        val itemIndex = rowIndex * colCount + colIndex
                                        if (itemIndex >= 0) {
                                            val item = if (pageIndex == coreAppsPageIndex) {
                                                pageItems.getOrNull(itemIndex - colCount)
                                            } else {
                                                pageItems.getOrNull(itemIndex)
                                            }
                                            
                                            if (item != null) {
                                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                                    if (settings.enableDesktopNotifications && pageIndex == 0) {
                                                        val notifications = NotificationRepository.activeNotifications
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
                                                                        if (settings.usePasscode) { pendingApp = item.info; showUnlockDialog = true }
                                                                        else { onAppLaunch(item.info); context.startActivity(Intent(context, SettingsActivity::class.java)) }
                                                                    } else if (item.info.packageName == context.packageName && firstScreenMap[AppType.WEATHER]?.contains(item.info.packageName) == true) {
                                                                        onAppLaunch(item.info); context.startActivity(Intent(context, WeatherActivity::class.java))
                                                                    } else if (isCameraApp) { onAppLaunch(item.info); AppUtils.launchCamera(context) }
                                                                    else if (isDialerApp) { onAppLaunch(item.info); AppUtils.launchDialer(context) }
                                                                    else if (isContactsApp) { onAppLaunch(item.info); AppUtils.launchContacts(context) }
                                                                    else { onAppLaunch(item.info); AppUtils.launchApp(context, item.info) }
                                                                }
                                                            }
                                                            is GridItem.SpeedDial -> {
                                                                SpeedDialItem(
                                                                    index = item.index, settings = settings, modifier = Modifier.fillMaxSize(),
                                                                    labelSize = labelSize, iconSizeMultiplier = settings.iconSizeMultiplier,
                                                                    iconShape = settings.iconShape, colorFilter = highContrastFilter
                                                                ) {
                                                                    val contact = settings.getSpeedDial(item.index)
                                                                    if (contact == null) {
                                                                        if (settings.usePasscode) { pendingSpeedDialIndex = item.index; showUnlockDialog = true }
                                                                        else { editingSpeedDialIndex = item.index; showAddContactDialog = true }
                                                                    } else {
                                                                        onSpeak(contact.first)
                                                                        val dialIntent = Intent(Intent.ACTION_CALL, "tel:${contact.second}".toUri())
                                                                        try { context.startActivity(dialIntent) }
                                                                        catch (_: Exception) { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${contact.second}".toUri())) }
                                                                    }
                                                                }
                                                            }
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
                        }
                    }
                }

                if (finalPages.size > 1) {
                    Row(modifier = Modifier.height(40.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        repeat(finalPages.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                            Box(modifier = Modifier.padding(4.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                        }
                    }
                }
            }

            if (settings.showVoiceAssistant) {
                FloatingActionButton(
                    onClick = {
                        if (settings.voiceAssistantMode == 1) { onVoiceAssistant() }
                        else {
                            AppUtils.launchSystemAssistant(context)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = navBarHeight + 16.dp, end = 16.dp),
                    containerColor = Color(0xFFFFD700), contentColor = Color.Black
                ) { Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.voice_assistant)) }
            }

            if (showUnlockDialog) {
                UnlockDialog(
                    settings = settings, pendingApp = pendingApp, pendingSpeedDialIndex = pendingSpeedDialIndex,
                    onUnlock = { app, index ->
                        if (app != null) { onAppLaunch(app); context.startActivity(Intent(context, SettingsActivity::class.java)) }
                        else if (index != -1) { editingSpeedDialIndex = index; showAddContactDialog = true }
                        else if (isAddingWeather) { showLocationSelectionDialog = true }
                        else { context.startActivity(Intent(context, SettingsActivity::class.java)) }
                    },
                    onDismiss = { showUnlockDialog = false; pendingApp = null; pendingSpeedDialIndex = -1; isAddingWeather = false }
                )
            }

            if (showLocationSelectionDialog) {
                LocationSelectionDialog(
                    settings = settings, onDismiss = { showLocationSelectionDialog = false; isAddingWeather = false },
                    onLocationAdded = { onRefreshWeather(); Toast.makeText(context, context.getString(R.string.location_added), Toast.LENGTH_SHORT).show() }
                )
            }

            if (showAddContactDialog) {
                AddContactDialog(editingSpeedDialIndex = editingSpeedDialIndex, settings = settings, onDismiss = { showAddContactDialog = false; editingSpeedDialIndex = -1 })
            }
        }
    }
}

@Composable
fun NotificationItem(sbn: StatusBarNotification) {
    val context = LocalContext.current
    val appName = remember(sbn.packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) {
            sbn.packageName
        }
    }
    val extras = sbn.notification.extras
    val title = extras.getString("android.title") ?: ""
    val text = extras.getCharSequence("android.text")?.toString() ?: ""
    val icon = sbn.notification.smallIcon ?: sbn.notification.getLargeIcon()

    Card(
        modifier = Modifier.fillMaxSize().clickable {
            try { sbn.notification.contentIntent?.send() } catch (_: Exception) {}
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                AsyncImage(
                    model = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = appName, fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(text = text, fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

private fun parseDesktopConfig(context: Context, xmlId: Int): DesktopConfig {
    val firstScreen = mutableListOf<DesktopXmlItem>()
    val secondScreen = mutableListOf<DesktopXmlItem>()
    try {
        val parser = context.resources.getXml(xmlId)
        var eventType = parser.eventType
        var currentTag: String? = null
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "first-screen", "second-screen" -> currentTag = parser.name
                    "app" -> {
                        val typeStr = parser.getAttributeValue(null, "type")
                        val pkg = parser.getAttributeValue(null, "package")
                        val item = if (typeStr != null) {
                            try { DesktopXmlItem.Type(AppType.valueOf(typeStr)) } catch (_: Exception) { null }
                        } else if (pkg != null) {
                            DesktopXmlItem.Package(pkg)
                        } else null
                        if (item != null) {
                            if (currentTag == "first-screen") firstScreen.add(item)
                            else if (currentTag == "second-screen") secondScreen.add(item)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (_: Exception) { }
    return DesktopConfig(firstScreen, secondScreen)
}

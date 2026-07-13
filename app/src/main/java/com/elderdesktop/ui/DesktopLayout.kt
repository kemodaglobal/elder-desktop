package com.elderdesktop.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.SettingsActivity
import com.elderdesktop.WeatherActivity
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.AppType
import com.elderdesktop.model.DesktopConfig
import com.elderdesktop.model.DesktopXmlItem
import com.elderdesktop.model.GridItem
import com.elderdesktop.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

@SuppressLint("SuspiciousIndentation")
@Composable
fun DesktopLayout(
    onAppLaunch: (AppInfo) -> Unit,
    onSpeak: (String) -> Unit,
    onVoiceAssistant: () -> Unit,
    weatherText: String,
    isWeatherAlert: Boolean,
    locationCity: String = "",
    currentTemperature: String = ""
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val insets = WindowInsets.systemBars.asPaddingValues()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var settingsVersion by remember { mutableIntStateOf(0) }
    val settings = remember(settingsVersion) { DesktopSettings(context) }

    val config = remember(settings.is2x3) {
        val currentCountry = Locale.getDefault().country
        val xmlId = if (settings.is2x3) {
            when (currentCountry) {
                "CN" -> R.xml.desktop_2x3_china
                "KP" -> R.xml.desktop_2x3_north_korea
                else -> R.xml.desktop_2x3_global
            }
        } else {
            when (currentCountry) {
                "CN" -> R.xml.desktop_3x4_china
                "KP" -> R.xml.desktop_3x4_north_korea
                else -> R.xml.desktop_3x4_global
            }
        }
        parseDesktopConfig(context, xmlId)
    }

    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var editingSpeedDialIndex by remember { mutableIntStateOf(-1) }
    var pendingSpeedDialIndex by remember { mutableIntStateOf(-1) }

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
            
            // Determine which apps we actually need to display
            val firstScreenMap = AppUtils.getFirstScreenPackageMap()
            val neededPackageNames = mutableSetOf<String>()

            if (settings.isBasicMode) {
                // In Basic Mode, only provide Gallery, Camera, Dialer, Contacts, and Settings
                listOf(
                    AppType.GALLERY, AppType.CAMERA, AppType.DIALER, AppType.CONTACTS, AppType.SETTINGS
                ).forEach { type ->
                    neededPackageNames.addAll(firstScreenMap[type] ?: emptyList())
                }
            } else {
                // XML first screen
                config.firstScreen.forEach { item ->
                    when(item) {
                        is DesktopXmlItem.Type -> neededPackageNames.addAll(firstScreenMap[item.type] ?: emptyList())
                        is DesktopXmlItem.Package -> neededPackageNames.add(item.packageName)
                    }
                }
                // XML second screen
                config.secondScreen.forEach { item ->
                    when(item) {
                        is DesktopXmlItem.Type -> neededPackageNames.addAll(firstScreenMap[item.type] ?: emptyList())
                        is DesktopXmlItem.Package -> neededPackageNames.add(item.packageName)
                    }
                }
                // User selected
                neededPackageNames.addAll(settings.selectedApps)
            }

            // Filter and process only needed apps
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
        val usedPackageNames = mutableSetOf<String>()

        fun resolveXmlItems(items: List<DesktopXmlItem>): List<AppInfo> {
            val resolved = mutableListOf<AppInfo>()
            for (item in items) {
                when (item) {
                    is DesktopXmlItem.Type -> {
                        val possiblePackages = firstScreenMap[item.type] ?: emptyList()
                        val foundApp = allApps.find { it.packageName in possiblePackages && it.packageName !in usedPackageNames }
                        if (foundApp != null) {
                            resolved.add(foundApp)
                            usedPackageNames.add(foundApp.packageName)
                        }
                    }
                    is DesktopXmlItem.Package -> {
                        val foundApp = allApps.find { it.packageName == item.packageName && it.packageName !in usedPackageNames }
                        if (foundApp != null) {
                            resolved.add(foundApp)
                            usedPackageNames.add(foundApp.packageName)
                        }
                    }
                }
            }
            return resolved
        }

        // 1. Identify Apps from XML "first-screen"
        val firstScreenFoundApps = resolveXmlItems(config.firstScreen)

        // 2. Identify Second screen apps (Region specific via XML packages/types)
        val secondScreenApps = resolveXmlItems(config.secondScreen)

        // 3. User selected apps
        val userSelectedApps = allApps.filter { it.packageName in settings.selectedApps && it.packageName !in usedPackageNames }

        // Construct Pages
        val finalPages = mutableListOf<List<GridItem>>()

        val rowCount = settings.layoutRows - 1
        val colCount = settings.layoutCols

        // PAGE 0: Contact Shortcuts
        val totalSpeedDials = rowCount * colCount
        val page0SpeedDials = (0 until totalSpeedDials).map { GridItem.SpeedDial(it) }
        finalPages.add(page0SpeedDials)

        // PAGE 1: First Screen Apps from XML
        val appsOnFirstPage = (rowCount - 1) * colCount
        val firstPageList = if (settings.isBasicMode) {
            // In basic mode, prioritize the core apps on page 1
            firstScreenFoundApps.take(appsOnFirstPage)
        } else {
            firstScreenFoundApps.take(appsOnFirstPage)
        }
        finalPages.add(firstPageList.map { GridItem.App(it) })

        // PAGE 2+: Designated apps (leftover first-screen, second-screen, and user-selected)
        val pageSize = rowCount * colCount
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
            initialPage = 1,
            pageCount = { finalPages.size }
        )
        val coroutineScope = rememberCoroutineScope()

        BackHandler {
            if (pagerState.currentPage != 1) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    val pageItems = finalPages[pageIndex]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        for (rowIndex in 0 until rowCount) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = if (rowIndex < rowCount - 1) 16.dp else 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (pageIndex == 1 && rowIndex == 0) {
                                    // Integrated Widget on Page 1
                                    if (!settings.isBasicMode) {
                                        ClockWidget(
                                            weatherText = weatherText,
                                            isWeatherAlert = isWeatherAlert,
                                            locationCity = locationCity,
                                            currentTemperature = currentTemperature,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(),
                                            onClick = {
                                                val intent = Intent(context, WeatherActivity::class.java)
                                                context.startActivity(intent)
                                            }
                                        )
                                    } else {
                                        // In Basic Mode, show simplified clock without weather
                                        SimpleClockWidget(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight()
                                        )
                                    }
                                } else {
                                    for (colIndex in 0 until colCount) {
                                        val itemIndex = calculateItemIndex(pageIndex, rowIndex, colIndex, colCount)

                                        if (itemIndex >= 0 && itemIndex < pageItems.size) {
                                            val item = pageItems[itemIndex]
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxSize()
                                            ) {
                                                when (item) {
                                                    is GridItem.App -> {
                                                        DesktopItem(
                                                            app = item.info,
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            val isSettingsApp = firstScreenMap[AppType.SETTINGS]?.contains(item.info.packageName) == true
                                                            val isCameraApp = firstScreenMap[AppType.CAMERA]?.contains(item.info.packageName) == true

                                                            if (isSettingsApp) {
                                                                if (settings.usePasscode) {
                                                                    pendingApp = item.info
                                                                    showUnlockDialog = true
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
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            val contact = settings.getSpeedDial(item.index)
                                                            if (contact == null) {
                                                                if (settings.usePasscode) {
                                                                    pendingSpeedDialIndex = item.index
                                                                    showUnlockDialog = true
                                                                } else {
                                                                    editingSpeedDialIndex = item.index
                                                                    showAddContactDialog = true
                                                                }
                                                            } else {
                                                                val name = contact.first
                                                                val number = contact.second
                                                                onSpeak(name)
                                                                val intent = Intent(Intent.ACTION_CALL, "tel:$number".toUri())
                                                                try {
                                                                    context.startActivity(intent)
                                                                } catch (_: Exception) {
                                                                    val dialIntent = Intent(Intent.ACTION_DIAL, "tel:$number".toUri())
                                                                    context.startActivity(dialIntent)
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

                if (finalPages.size > 1) {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(finalPages.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }

            if (settings.showVoiceAssistant) {
                FloatingActionButton(
                    onClick = {
                        if (settings.voiceAssistantMode == 1) {
                            onVoiceAssistant()
                        } else {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                try {
                                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "无法启动语音助手", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            bottom = navBarHeight + 16.dp,
                            end = 16.dp
                        ),
                    containerColor = Color(0xFFFFD700), // High-visibility Gold/Yellow
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.voice_assistant))
                }
            }

            if (showUnlockDialog) {
                UnlockDialog(
                    settings = settings,
                    pendingApp = pendingApp,
                    pendingSpeedDialIndex = pendingSpeedDialIndex,
                    onUnlock = { app, index ->
                        if (app != null) {
                            onAppLaunch(app)
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        } else if (index != -1) {
                            editingSpeedDialIndex = index
                            showAddContactDialog = true
                        }
                    },
                    onDismiss = {
                        showUnlockDialog = false
                        pendingApp = null
                        pendingSpeedDialIndex = -1
                    }
                )
            }

            if (showAddContactDialog) {
                AddContactDialog(
                    editingSpeedDialIndex = editingSpeedDialIndex,
                    settings = settings,
                    onDismiss = {
                        showAddContactDialog = false
                        editingSpeedDialIndex = -1
                    }
                )
            }
        }
    }
}

private fun calculateItemIndex(pageIndex: Int, rowIndex: Int, colIndex: Int, colCount: Int): Int {
    return when (pageIndex) {
        0 -> rowIndex * colCount + colIndex
        1 -> (rowIndex - 1) * colCount + colIndex
        else -> rowIndex * colCount + colIndex
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

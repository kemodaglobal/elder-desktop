package com.elderdesktop.ui

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
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
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.AppType
import com.elderdesktop.model.DesktopConfig
import com.elderdesktop.model.GridItem
import com.elderdesktop.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import org.xmlpull.v1.XmlPullParser
import java.util.*

@Composable
fun DesktopLayout(
    onAppLaunch: (AppInfo) -> Unit,
    onSpeak: (String) -> Unit,
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

    val config = remember {
        val currentCountry = Locale.getDefault().country
        val xmlId = when (currentCountry) {
            "CN" -> R.xml.desktop_2x3_china
            "KP" -> R.xml.desktop_2x3_north_korea
            else -> R.xml.desktop_2x3_global
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
            val apps = AppUtils.getLaunchableApps(context)
            apps.map { app ->
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

        // 1. Core Apps for Page 0
        val coreTypes = listOf(AppType.GALLERY, AppType.CAMERA, AppType.DIALER, AppType.MESSAGING)
        val coreApps = mutableListOf<AppInfo>()
        val usedPackageNames = mutableSetOf<String>()

        for (type in coreTypes) {
            val possiblePackages = firstScreenMap[type] ?: emptyList()
            val foundApp = allApps.find { it.packageName in possiblePackages && it.packageName !in usedPackageNames }
            if (foundApp != null) {
                coreApps.add(foundApp)
                usedPackageNames.add(foundApp.packageName)
            }
        }

        // 2. First screen apps
        val firstScreenApps = mutableListOf<AppInfo>()
        for (type in config.firstScreenTypes) {
            if (type in coreTypes) continue
            val possiblePackages = firstScreenMap[type] ?: emptyList()
            val foundApp = allApps.find { it.packageName in possiblePackages && it.packageName !in usedPackageNames }
            if (foundApp != null) {
                firstScreenApps.add(foundApp)
                usedPackageNames.add(foundApp.packageName)
            }
        }

        // 3. Second screen apps
        val secondScreenApps = allApps.filter {
            it.packageName in config.secondScreenPackages && it.packageName !in usedPackageNames
        }
        secondScreenApps.forEach { usedPackageNames.add(it.packageName) }

        // 4. Remaining apps
        val remainingApps = allApps.filter { it.packageName !in usedPackageNames }

        // Construct Pages
        val finalPages = mutableListOf<List<GridItem>>()

        // PAGE 0: Core Apps (4 apps)
        finalPages.add(coreApps.take(4).map { GridItem.App(it) })

        // PAGE 1: Contact Shortcuts (6 slots)
        val page1SpeedDials = (0 until 6).map { GridItem.SpeedDial(it) }
        finalPages.add(page1SpeedDials)

        // PAGE 2+: Everything else
        val pageSize = 6
        val everythingElse = mutableListOf<GridItem>()
        everythingElse.addAll(firstScreenApps.map { GridItem.App(it) })
        everythingElse.addAll(secondScreenApps.map { GridItem.App(it) })
        everythingElse.addAll(remainingApps.map { GridItem.App(it) })

        if (everythingElse.isNotEmpty()) {
            finalPages.addAll(everythingElse.chunked(pageSize))
        }

        val pagerState = rememberPagerState(pageCount = { finalPages.size })
        val coroutineScope = rememberCoroutineScope()

        BackHandler {
            if (pagerState.currentPage > 0) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (pagerState.currentPage == 0) {
                    ClockWidget(
                        weatherText = weatherText,
                        isWeatherAlert = isWeatherAlert,
                        locationCity = locationCity,
                        currentTemperature = currentTemperature,
                    )
                } else {
                    Spacer(modifier = Modifier.height(180.dp))
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    val pageItems = finalPages[pageIndex]
                    val rowCount = if (pageIndex == 0) 2 else 3
                    val colCount = 2

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (rowIndex in 0 until rowCount) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                for (colIndex in 0 until colCount) {
                                    val itemIndex = rowIndex * colCount + colIndex
                                    if (itemIndex < pageItems.size) {
                                        val item = pageItems[itemIndex]
                                        Box(modifier = Modifier.weight(1f)) {
                                            when (item) {
                                                is GridItem.App -> {
                                                    DesktopItem(item.info, itemHeight = 0.dp) { // itemHeight ignored with fillMaxSize
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
                                                    SpeedDialItem(item.index, settings, itemHeight = 0.dp) {
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

            // Voice Assistant Button
            if (settings.showVoiceAssistant) {
                FloatingActionButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            bottom = navBarHeight + 16.dp,
                            end = 16.dp
                        ),
                    containerColor = Color.White.copy(alpha = 0.7f),
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

private fun parseDesktopConfig(context: Context, xmlId: Int): DesktopConfig {
    val firstScreen = mutableListOf<AppType>()
    val secondScreen = mutableListOf<String>()
    try {
        val parser = context.resources.getXml(xmlId)
        var eventType = parser.eventType
        var currentTag: String? = null
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "first-screen", "second-screen" -> currentTag = parser.name
                    "app" -> {
                        if (currentTag == "first-screen") {
                            val typeStr = parser.getAttributeValue(null, "type")
                            if (typeStr != null) {
                                try { firstScreen.add(AppType.valueOf(typeStr)) } catch (_: Exception) {}
                            }
                        } else if (currentTag == "second-screen") {
                            val pkg = parser.getAttributeValue(null, "package")
                            if (pkg != null) secondScreen.add(pkg)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (_: Exception) { }
    return DesktopConfig(firstScreen, secondScreen)
}
package com.elderdesktop.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
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
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

@Composable
fun DesktopLayout(
    onAppLaunch: (AppInfo) -> Unit,
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

        // PAGE 0: Contact Shortcuts (6 slots) - 现在是首页
		val page0SpeedDials = (0 until 6).map { GridItem.SpeedDial(it) }
		finalPages.add(page0SpeedDials)

        // PAGE 1: Core Apps (4 apps) - 现在是第二页
		finalPages.add(coreApps.take(4).map { GridItem.App(it) })

        // PAGE 2+: Everything else
        val pageSize = 6
        val everythingElse = mutableListOf<GridItem>()
        everythingElse.addAll(firstScreenApps.map { GridItem.App(it) })
        everythingElse.addAll(secondScreenApps.map { GridItem.App(it) })
        everythingElse.addAll(remainingApps.map { GridItem.App(it) })

        if (everythingElse.isNotEmpty()) {
            finalPages.addAll(everythingElse.chunked(pageSize))
        }

        val pagerState = rememberPagerState(
            initialPage = 1,
            pageCount = { finalPages.size }
        )
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    val pageItems = finalPages[pageIndex]
                    val rowCount = 3
                    val colCount = 2

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Top  // ★ 改为 Top
                    ) {
                        for (rowIndex in 0 until rowCount) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = if (rowIndex < rowCount - 1) 16.dp else 0.dp),  // ★ 手动间距
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (pageIndex == 1 && rowIndex == 0) {
                                    // ClockWidget
                                    ClockWidget(
                                        weatherText = weatherText,
                                        isWeatherAlert = isWeatherAlert,
                                        locationCity = locationCity,
                                        currentTemperature = currentTemperature,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                    )
                                } else {
                                    for (colIndex in 0 until colCount) {
                                        val itemIndex = calculateItemIndex(pageIndex, rowIndex, colIndex)

                                        if (itemIndex >= 0 && itemIndex < pageItems.size) {
                                            val item = pageItems[itemIndex]
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxSize()  // ★ 确保占满
                                            ) {
                                                when (item) {
                                                    is GridItem.App -> {
                                                        DesktopItem(
                                                            item.info
                                                            // ★ DesktopItem 填满
                                                        ) {
                                                            Log.d("DesktopLayout", "Clicked: ${item.info.label}")
                                                            // ... 原有点击逻辑
                                                        }
                                                    }
                                                    is GridItem.SpeedDial -> {
                                                        SpeedDialItem(
                                                            item.index,
                                                            settings,
                                                            modifier = Modifier.fillMaxSize()  // ★ SpeedDialItem 填满
                                                        ) {
                                                            Log.d("DesktopLayout", "Clicked SpeedDial: ${item.index}")
                                                            // ... 原有点击逻辑
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

            // Voice Assistant Button
            // Voice Assistant Button
            if (settings.showVoiceAssistant) {
                FloatingActionButton(
                    onClick = {
                        try {
                            // 方式1: 使用系统语音搜索 Intent
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            try {
                                // 方式2: 使用系统语音命令 Intent
                                val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    // 方式3: 使用 Google 语音搜索
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

// 辅助函数：计算实际 item 索引
private fun calculateItemIndex(pageIndex: Int, rowIndex: Int, colIndex: Int): Int {
    return when (pageIndex) {
        0 -> {
            // Page 0: 速拨页，3行，正常计算
            rowIndex * 2 + colIndex
        }
        1 -> {
            // Page 1: 核心应用页，第0行被 ClockWidget 占用
            // 第1行(col0=0, col1=1)，第2行(col0=2, col1=3)
            (rowIndex - 1) * 2 + colIndex
        }
        else -> {
            // 其他页：正常计算
            rowIndex * 2 + colIndex
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
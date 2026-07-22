package com.elderdesktop.ui

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.SettingsActivity
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.GridItem
import com.elderdesktop.model.PermissionRationaleProvider
import com.elderdesktop.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            
            // Migration/Linkage: Populate layoutOrder if empty
            if (settings.layoutOrder.isEmpty()) {
                val initialOrder = AppUtils.generateDefaultLayout(context, allLaunchable).toMutableList()
                val used = initialOrder.filter { it.startsWith("app:") }.map { it.removePrefix("app:") }.toSet()
                settings.selectedApps.forEach { pkg ->
                    if (pkg !in used && allLaunchable.any { it.packageName == pkg }) {
                        initialOrder.add("app:$pkg")
                    }
                }
                settings.layoutOrder = initialOrder
            }

            // Ensure selectedApps is in sync with layoutOrder for linkage
            val currentSelectedInOrder = settings.layoutOrder
                .filter { it.startsWith("app:") }
                .map { it.removePrefix("app:") }
                .toSet()
            if (settings.selectedApps != currentSelectedInOrder) {
                settings.selectedApps = currentSelectedInOrder
            }

            allLaunchable.map { app ->
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
        val finalItems = remember(settings.layoutOrder, allApps) {
            settings.layoutOrder.mapNotNull { item ->
                if (item.startsWith("app:")) {
                    val pkg = item.removePrefix("app:")
                    allApps.find { it.packageName == pkg }?.let { GridItem.App(it) }
                } else if (item == "widget:clock") {
                    GridItem.ClockWidget
                } else null
            }
        }

        val effectiveRows = rowCount - 1
        val finalPages = mutableListOf<List<List<GridItem?>>>()

        // Page 0: Notifications
        if (settings.enableDesktopNotifications && !settings.enableFloatingNotifications) {
            val notificationRows = (0 until effectiveRows).map {
                (0 until colCount).map { GridItem.SpeedDial(-2) }
            }
            finalPages.add(notificationRows)
        }

        // Page 1: Contacts (Speed Dial)
        if (!settings.isScrollingMode) {
            val contactRows = (0 until effectiveRows).map { r ->
                (0 until colCount).map { c -> GridItem.SpeedDial(r * colCount + c) }
            }
            finalPages.add(contactRows)
        }

        // Apps & Widget Paging with Row-based packing
        val appRows = mutableListOf<List<GridItem?>>()
        var currentRow = mutableListOf<GridItem?>()
        for (item in finalItems) {
            if (item is GridItem.ClockWidget) {
                if (currentRow.isNotEmpty()) {
                    while (currentRow.size < colCount) currentRow.add(null)
                    appRows.add(currentRow)
                    currentRow = mutableListOf()
                }
                val widgetRow = mutableListOf<GridItem?>()
                widgetRow.add(GridItem.ClockWidget)
                while (widgetRow.size < colCount) widgetRow.add(null)
                appRows.add(widgetRow)
            } else {
                currentRow.add(item)
                if (currentRow.size == colCount) {
                    appRows.add(currentRow)
                    currentRow = mutableListOf()
                }
            }
        }
        if (currentRow.isNotEmpty()) {
            while (currentRow.size < colCount) currentRow.add(null)
            appRows.add(currentRow)
        }

        finalPages.addAll(appRows.chunked(effectiveRows))

        val pagerState = rememberPagerState(
            initialPage = if (settings.enableDesktopNotifications && !settings.enableFloatingNotifications) 2 else 1,
            pageCount = { finalPages.size }
        )
        val coroutineScope = rememberCoroutineScope()

        BackHandler {
            val target = if (settings.enableDesktopNotifications && !settings.enableFloatingNotifications) 2 else 1
            if (pagerState.currentPage != target) {
                coroutineScope.launch { pagerState.animateScrollToPage(target) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .background(if (isHighContrast) MaterialTheme.colorScheme.background else Color.Transparent)
        ) {
            val firstScreenMap = AppUtils.getFirstScreenPackageMap()

            if (settings.isScrollingMode) {
                // Vertical Scrolling Layout
                val rowHeight = (configuration.screenWidthDp / colCount).coerceIn(120, 240).dp
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    val allFlattenedRows = finalPages.flatten()
                    itemsIndexed(
                        items = allFlattenedRows,
                        key = { index, row -> 
                            when (val firstItem = row.firstOrNull()) {
                                is GridItem.App -> "app_${firstItem.info.packageName}_$index"
                                is GridItem.ClockWidget -> "widget_clock_$index"
                                is GridItem.SpeedDial -> "speed_${firstItem.index}_$index"
                                else -> "empty_$index"
                            }
                        }
                    ) { rowIndex, rowItems ->
                        DesktopRow(
                            rowItems = rowItems,
                            rowIndex = rowIndex,
                            colCount = colCount,
                            modifier = Modifier.height(rowHeight).padding(bottom = 16.dp),
                            labelSize = labelSize,
                            highContrastFilter = highContrastFilter,
                            settings = settings,
                            firstScreenMap = firstScreenMap,
                            weatherText = weatherText,
                            isWeatherAlert = isWeatherAlert,
                            locationCity = locationCity,
                            currentTemperature = currentTemperature,
                            weatherCode = weatherCode,
                            onAppLaunch = onAppLaunch,
                            onSpeak = onSpeak,
                            showUnlockDialog = { showUnlockDialog = true },
                            setIsAddingWeather = { isAddingWeather = it },
                            setPendingApp = { pendingApp = it },
                            setPendingSpeedDialIndex = { pendingSpeedDialIndex = it },
                            setEditingSpeedDialIndex = { editingSpeedDialIndex = it },
                            showAddContactDialog = { showAddContactDialog = true },
                            showLocationSelectionDialog = { showLocationSelectionDialog = true }
                        )
                    }
                }
            } else {
                // Paging Layout
                Column(modifier = Modifier.fillMaxSize().padding(insets)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                        val pageRows = finalPages[pageIndex]
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Top) {
                            pageRows.forEachIndexed { rowIndex, rowItems ->
                                DesktopRow(
                                    rowItems = rowItems,
                                    rowIndex = rowIndex,
                                    colCount = colCount,
                                    modifier = Modifier.weight(1f).padding(bottom = 16.dp),
                                    labelSize = labelSize,
                                    highContrastFilter = highContrastFilter,
                                    settings = settings,
                                    firstScreenMap = firstScreenMap,
                                    weatherText = weatherText,
                                    isWeatherAlert = isWeatherAlert,
                                    locationCity = locationCity,
                                    currentTemperature = currentTemperature,
                                    weatherCode = weatherCode,
                                    onAppLaunch = onAppLaunch,
                                    onSpeak = onSpeak,
                                    showUnlockDialog = { showUnlockDialog = true },
                                    setIsAddingWeather = { isAddingWeather = it },
                                    setPendingApp = { pendingApp = it },
                                    setPendingSpeedDialIndex = { pendingSpeedDialIndex = it },
                                    setEditingSpeedDialIndex = { editingSpeedDialIndex = it },
                                    showAddContactDialog = { showAddContactDialog = true },
                                    showLocationSelectionDialog = { showLocationSelectionDialog = true }
                                )
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
            }

            if (settings.showVoiceAssistant) {
                FloatingActionButton(
                    onClick = {
                        if (settings.voiceAssistantMode == 1) { onVoiceAssistant() }
                        else { AppUtils.launchSystemAssistant(context) }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = navBarHeight + 16.dp, end = 16.dp),
                    containerColor = Color(0xFFFFD700), contentColor = Color.Black
                ) { Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.voice_assistant)) }
            }

            if (showUnlockDialog) {
                UnlockDialog(
                    settings = settings, pendingApp = pendingApp, pendingSpeedDialIndex = pendingSpeedDialIndex,
                    onUnlock = { app, index ->
                        val options = ActivityOptions.makeCustomAnimation(context, R.anim.fade_in, R.anim.fade_out).toBundle()
                        if (app != null) { 
                            onAppLaunch(app)
                            context.startActivity(Intent(context, SettingsActivity::class.java), options) 
                        }
                        else if (index != -1) { editingSpeedDialIndex = index; showAddContactDialog = true }
                        else if (isAddingWeather) { showLocationSelectionDialog = true }
                        else { context.startActivity(Intent(context, SettingsActivity::class.java), options) }
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

            PermissionPurposePrompt(
                rationale = PermissionRationaleProvider.rationaleMessage,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

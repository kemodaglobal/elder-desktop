package com.elderdesktop.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import coil3.compose.rememberAsyncImagePainter
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo
import com.elderdesktop.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { DesktopSettings(context) }
    val scrollState = rememberScrollState()

    // Fix for older Android versions auto-scrolling to bottom on open
    LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
    }
    
    var showVoice by remember { mutableStateOf(settings.showVoiceAssistant) }
    var voiceMode by remember { mutableIntStateOf(settings.voiceAssistantMode) }
    var voiceAnnouncements by remember { mutableStateOf(settings.voiceAnnouncements) }
    var usePasscode by remember { mutableStateOf(settings.usePasscode) }
    var isBasicMode by remember { mutableStateOf(settings.isBasicMode) }
    var passcode by remember { mutableStateOf(settings.passcode) }
    var showPasscodeDialog by remember { mutableStateOf(false) }

    var speedDialUpdateTrigger by remember { mutableIntStateOf(0) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedApps by remember { mutableStateOf(settings.selectedApps) }
    
    var showAboutPage by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    fun isNotificationServiceEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    if (showAboutPage) {
        AlertDialog(
            onDismissRequest = { showAboutPage = false },
            title = { Text(stringResource(R.string.about)) },
            text = { AboutPage() },
            confirmButton = {
                Button(onClick = { showAboutPage = false }) {
                    Text(stringResource(R.string.back))
                }
            }
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text(stringResource(R.string.privacy_policy)) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = androidx.core.text.HtmlCompat.fromHtml(
                            stringResource(R.string.privacy_policy_content),
                            androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
                        ).toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicy = false }) {
                    Text(stringResource(R.string.back))
                }
            }
        )
    }

    LaunchedEffect(showAppSelectionDialog) {
        if (showAppSelectionDialog && allApps.isEmpty()) {
            allApps = withContext(Dispatchers.IO) {
                AppUtils.getLaunchableApps(context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ====== 1. 显示与布局 / Display & Layout ======
        Text(stringResource(R.string.layout), style = MaterialTheme.typography.titleLarge)
        
        val configuration = LocalConfiguration.current
        val sw = configuration.smallestScreenWidthDp
        
        // Grid Selection
        if (sw < 600) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.is2x3,
                    onClick = {
                        settings.use2x3()
                        speedDialUpdateTrigger++
                    }
                )
                Text(stringResource(R.string.layout_2x3), modifier = Modifier.padding(start = 8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.is3x4,
                    onClick = {
                        settings.use3x4()
                        speedDialUpdateTrigger++
                    }
                )
                Text(stringResource(R.string.layout_3x4), modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.is4x3,
                    onClick = {
                        settings.use4x3()
                        speedDialUpdateTrigger++
                    }
                )
                Text(stringResource(R.string.layout_4x3), modifier = Modifier.padding(start = 8.dp))
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.is6x4,
                    onClick = {
                        settings.use6x4()
                        speedDialUpdateTrigger++
                    }
                )
                Text(stringResource(R.string.layout_6x4), modifier = Modifier.padding(start = 8.dp))
            }
        }

        // Dedicated layout for dual-screen/rotatable requirements
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = settings.is3x2,
                onClick = {
                    settings.use3x2()
                    speedDialUpdateTrigger++
                }
            )
            Text(stringResource(R.string.layout_3x2), modifier = Modifier.padding(start = 8.dp))
        }

        // Font Size
        Column {
            Text(stringResource(R.string.font_size))
            var fontSize by remember { mutableFloatStateOf(settings.fontSizeMultiplier) }
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                onValueChangeFinished = { settings.fontSizeMultiplier = fontSize },
                valueRange = 0.8f..1.5f,
                steps = 5
            )
        }

        // Icon Size
        Column {
            Text(stringResource(R.string.icon_size))
            var iconSize by remember { mutableFloatStateOf(settings.iconSizeMultiplier) }
            Slider(
                value = iconSize,
                onValueChange = { iconSize = it },
                onValueChangeFinished = { settings.iconSizeMultiplier = iconSize },
                valueRange = 0.8f..1.5f,
                steps = 5
            )
        }

        // Icon Shape
        Column {
            Text(stringResource(R.string.icon_shape))
            val shapes = listOf("rounded", "circle", "square", "native")
            val shapeLabels = listOf(
                R.string.shape_rounded,
                R.string.shape_circle,
                R.string.shape_square,
                R.string.shape_native
            )
            var selectedShape by remember { mutableStateOf(settings.iconShape) }
            shapes.forEachIndexed { index, shape ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedShape == shape,
                        onClick = {
                            selectedShape = shape
                            settings.iconShape = shape
                            speedDialUpdateTrigger++ // Refresh icons
                        }
                    )
                    Text(stringResource(shapeLabels[index]), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        // High Contrast
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.high_contrast))
            var highContrast by remember { mutableStateOf(settings.highContrastMode) }
            Switch(
                checked = highContrast,
                onCheckedChange = {
                    highContrast = it
                    settings.highContrastMode = it
                }
            )
        }

        // Theme Selection
        Column {
            Text(stringResource(R.string.theme_selection), style = MaterialTheme.typography.bodyLarge)
            val themes = listOf(
                "classic" to R.string.theme_classic,
                "emerald" to R.string.theme_emerald,
                "rose" to R.string.theme_rose,
                "orange" to R.string.theme_orange,
                "high_contrast" to R.string.theme_high_contrast
            )
            var selectedTheme by remember { mutableStateOf(settings.themeChoice) }
            themes.forEach { (id, labelRes) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedTheme == id,
                        onClick = {
                            selectedTheme = id
                            settings.themeChoice = id
                        }
                    )
                    Text(stringResource(labelRes), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        // Font Selection
        Column {
            Text(stringResource(R.string.font_selection), style = MaterialTheme.typography.bodyLarge)
            val fonts = listOf(
                "default" to R.string.font_default,
                "serif" to R.string.font_serif,
                "monospace" to R.string.font_monospace
            )
            var selectedFont by remember { mutableStateOf(settings.fontChoice) }
            fonts.forEach { (id, labelRes) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedFont == id,
                        onClick = {
                            selectedFont = id
                            settings.fontChoice = id
                        }
                    )
                    Text(stringResource(labelRes), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 2. 应用与联系人 / Apps & Contacts ======
        Text(stringResource(R.string.manage_apps), style = MaterialTheme.typography.titleLarge)
        
        Button(
            onClick = { showAppSelectionDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.select_apps))
        }

        val userApps = remember(allApps, selectedApps) {
            allApps.filter { it.packageName in selectedApps }
        }

        if (userApps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            userApps.forEach { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(app.label, modifier = Modifier.padding(start = 8.dp))
                        IconButton(onClick = {
                            val newSelected = selectedApps.toMutableSet()
                            newSelected.remove(app.packageName)
                            selectedApps = newSelected
                            settings.selectedApps = newSelected
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFE74C3C))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.add_contact), style = MaterialTheme.typography.titleLarge)
        
        val totalSpeedDials = (settings.layoutRows - 1) * settings.layoutCols
        key(speedDialUpdateTrigger) {
            for (i in 0 until totalSpeedDials) {
                val contact = settings.getSpeedDial(i)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (contact?.third != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(contact.third!!.toUri()),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Text(
                                    text = contact?.first ?: stringResource(R.string.add_contact),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (contact != null) {
                                    Text(
                                        text = contact.second,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        
                        Row {
                            IconButton(onClick = {
                                editingIndex = i
                                showEditContactDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_contact))
                            }
                            if (contact != null) {
                                IconButton(onClick = {
                                    settings.clearSpeedDial(i)
                                    speedDialUpdateTrigger++
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFE74C3C))
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 3. 语音与 AI / Voice & AI ======
        Text(stringResource(R.string.features), style = MaterialTheme.typography.titleLarge)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.voice_assistant_button))
            Switch(
                checked = showVoice,
                onCheckedChange = {
                    showVoice = it
                    settings.showVoiceAssistant = it
                }
            )
        }

        if (showVoice) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(stringResource(R.string.voice_assistant_mode), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = voiceMode == 0,
                        onClick = {
                            voiceMode = 0
                            settings.voiceAssistantMode = 0
                        }
                    )
                    Text(stringResource(R.string.voice_assistant_system), modifier = Modifier.padding(start = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = voiceMode == 1,
                        onClick = {
                            voiceMode = 1
                            settings.voiceAssistantMode = 1
                        }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(R.string.voice_assistant_engine))
                        Text(
                            text = stringResource(R.string.voice_assistant_engine_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.voice_announcements))
            Switch(
                checked = voiceAnnouncements,
                onCheckedChange = {
                    voiceAnnouncements = it
                    settings.voiceAnnouncements = it
                }
            )
        }

        Column {
            Text(stringResource(R.string.speech_rate))
            var speechRate by remember { mutableFloatStateOf(settings.speechRate) }
            Slider(
                value = speechRate,
                onValueChange = { speechRate = it },
                onValueChangeFinished = { settings.speechRate = speechRate },
                valueRange = 0.5f..2.0f,
                steps = 5
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ai_chat))
                Text(
                    text = stringResource(R.string.ai_chat_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            var enableAI by remember { mutableStateOf(settings.enableDeepSeek) }
            Switch(
                checked = enableAI,
                onCheckedChange = {
                    enableAI = it
                    settings.enableDeepSeek = it
                }
            )
        }

        if (settings.enableDeepSeek) {
            Text(stringResource(R.string.ai_provider), style = MaterialTheme.typography.bodyMedium)
            var currentProvider by remember { mutableStateOf(settings.aiProvider) }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                listOf(
                    "deepseek" to "DeepSeek",
                    "openai" to "ChatGPT",
                    "google" to "Google AI (Gemini)"
                ).forEach { (id, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = currentProvider == id,
                            onClick = {
                                currentProvider = id
                                settings.aiProvider = id
                            }
                        )
                        Text(name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            when (currentProvider) {
                "deepseek" -> {
                    var apiKey by remember { mutableStateOf(settings.deepSeekApiKey) }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { newValue ->
                            apiKey = newValue
                            settings.deepSeekApiKey = newValue
                        },
                        label = { Text(stringResource(R.string.deepseek_api_key)) },
                        placeholder = { Text(stringResource(R.string.enter_api_key)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                "openai" -> {
                    var apiKey by remember { mutableStateOf(settings.openAiApiKey) }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { newValue ->
                            apiKey = newValue
                            settings.openAiApiKey = newValue
                        },
                        label = { Text(stringResource(R.string.openai_api_key)) },
                        placeholder = { Text(stringResource(R.string.enter_api_key)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                "google" -> {
                    var apiKey by remember { mutableStateOf(settings.googleAiApiKey) }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { newValue ->
                            apiKey = newValue
                            settings.googleAiApiKey = newValue
                        },
                        label = { Text(stringResource(R.string.google_ai_api_key)) },
                        placeholder = { Text(stringResource(R.string.enter_api_key)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 4. 通知与系统 / Notifications & System ======
        Text(stringResource(R.string.system_settings), style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.basic_mode))
                Text(
                    text = stringResource(R.string.basic_mode_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isBasicMode,
                onCheckedChange = {
                    isBasicMode = it
                    settings.isBasicMode = it
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.desktop_notifications))
                Text(
                    text = stringResource(R.string.desktop_notifications_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            var enableNotifications by remember { mutableStateOf(settings.enableDesktopNotifications) }
            Switch(
                checked = enableNotifications,
                onCheckedChange = {
                    if (it) {
                        if (!isNotificationServiceEnabled()) {
                            showNotificationPermissionDialog = true
                        } else if (!isOverlayPermissionGranted()) {
                            showOverlayPermissionDialog = true
                        } else {
                            enableNotifications = true
                            settings.enableDesktopNotifications = true
                        }
                    } else {
                        enableNotifications = false
                        settings.enableDesktopNotifications = false
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.prevent_sleep))
                Text(
                    text = stringResource(R.string.prevent_sleep_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            var preventSleep by remember { mutableStateOf(settings.preventSleep) }
            Switch(
                checked = preventSleep,
                onCheckedChange = {
                    preventSleep = it
                    settings.preventSleep = it
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 5. 安全与隐私 / Security & Privacy ======
        Text(stringResource(R.string.peace_of_mind_lock), style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.peace_of_mind_lock))
            Switch(
                checked = usePasscode,
                onCheckedChange = {
                    if (it && passcode.isEmpty()) {
                        showPasscodeDialog = true
                    } else {
                        usePasscode = it
                        settings.usePasscode = it
                    }
                }
            )
        }

        if (usePasscode) {
            TextButton(onClick = { showPasscodeDialog = true }) {
                Text(stringResource(R.string.change_passcode, passcode))
            }
        }

        TextButton(
            onClick = { showPrivacyPolicy = true },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(stringResource(R.string.privacy_policy))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 6. 天气 / Weather ======
        Text(stringResource(R.string.weather), style = MaterialTheme.typography.titleLarge)
        
        Text(stringResource(R.string.weather_provider), style = MaterialTheme.typography.bodyMedium)
        var currentWeatherProvider by remember { mutableStateOf(settings.weatherProvider) }
        var weatherKey by remember { mutableStateOf(settings.weatherApiKey) }
        
        Column(modifier = Modifier.padding(start = 16.dp)) {
            listOf(
                "open-meteo" to "Open-Meteo (Free)",
                "qweather" to "QWeather (HeWeather)",
                "openweather" to "OpenWeatherMap"
            ).forEach { (id, name) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = currentWeatherProvider == id,
                        onClick = {
                            currentWeatherProvider = id
                            settings.weatherProvider = id
                        }
                    )
                    Text(name, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        if (currentWeatherProvider != "open-meteo") {
            OutlinedTextField(
                value = weatherKey,
                onValueChange = { newValue ->
                    weatherKey = newValue
                    settings.weatherApiKey = newValue
                },
                label = { Text(stringResource(R.string.weather_api_key)) },
                placeholder = { Text(stringResource(R.string.enter_api_key)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        val manualLocations = settings.manualWeatherLocations
        if (manualLocations.isNotEmpty()) {
            manualLocations.forEach { loc ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(loc, modifier = Modifier.padding(start = 8.dp))
                        IconButton(onClick = {
                            settings.removeWeatherLocation(loc)
                            speedDialUpdateTrigger++ // Reuse trigger to refresh
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFE74C3C))
                        }
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.no_manual_locations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 7. 其他 / Others ======
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22))
        ) {
            Text(stringResource(R.string.system_settings), color = Color.White)
        }

        Button(
            onClick = { showAboutPage = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(stringResource(R.string.about), color = Color.White)
        }
    }

    // ====== 对话框 ======
    if (showPasscodeDialog) {
        PasscodeDialog(
            onDismiss = { showPasscodeDialog = false },
            onSave = { newPasscode ->
                passcode = newPasscode
                settings.passcode = newPasscode
                usePasscode = true
                settings.usePasscode = true
                showPasscodeDialog = false
            }
        )
    }

    if (showEditContactDialog) {
        EditContactDialog(
            index = editingIndex,
            settings = settings,
            onDismiss = { showEditContactDialog = false },
            onSaved = { speedDialUpdateTrigger++ }
        )
    }

    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = { Text(stringResource(R.string.notification_permission_required)) },
            text = { Text(stringResource(R.string.notification_permission_desc)) },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    showNotificationPermissionDialog = false
                }) {
                    Text(stringResource(R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text(stringResource(R.string.overlay_permission_required)) },
            text = { Text(stringResource(R.string.overlay_permission_desc)) },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
                    showOverlayPermissionDialog = false
                }) {
                    Text(stringResource(R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAppSelectionDialog) {
        AppSelectionDialog(
            allApps = allApps,
            selectedApps = selectedApps,
            onSelectionChanged = { selectedApps = it },
            onDismiss = { showAppSelectionDialog = false },
            onSave = {
                settings.selectedApps = selectedApps
                showAppSelectionDialog = false
            }
        )
    }
}

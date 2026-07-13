package com.elderdesktop.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    
    var is2x3 by remember { mutableStateOf(settings.is2x3) }
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ====== 布局选择 ======
        Text(stringResource(R.string.layout), style = MaterialTheme.typography.titleLarge)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = is2x3,
                onClick = {
                    is2x3 = true
                    settings.use2x3()
                    speedDialUpdateTrigger++
                }
            )
            Text(stringResource(R.string.layout_2x3), modifier = Modifier.padding(start = 8.dp))
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = !is2x3,
                onClick = {
                    is2x3 = false
                    settings.use3x4()
                    speedDialUpdateTrigger++
                }
            )
            Text(stringResource(R.string.layout_3x4), modifier = Modifier.padding(start = 8.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 应用管理 ======
        Text(stringResource(R.string.manage_apps), style = MaterialTheme.typography.titleLarge)
        
        Button(
            onClick = { showAppSelectionDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.select_apps))
        }

        // Display currently selected apps for easy removal
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(app.label, modifier = Modifier.padding(start = 8.dp))
                        }
                        IconButton(onClick = {
                            val newSelected = selectedApps.toMutableSet()
                            newSelected.remove(app.packageName)
                            selectedApps = newSelected
                            settings.selectedApps = newSelected
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = Color(0xFFE74C3C)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 联系人管理 ======
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
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (contact?.third != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(contact.third!!.toUri()),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
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
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = Color(0xFFE74C3C)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 功能开关 ======
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 手动天气位置 ======
        Text(stringResource(R.string.weather), style = MaterialTheme.typography.titleLarge)
        
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

        // ====== 系统设置 ======
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22))
        ) {
            Text(stringResource(R.string.system_settings), color = Color.White)
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
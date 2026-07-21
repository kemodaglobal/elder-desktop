package com.elderdesktop.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R

@Composable
fun SafetySystemSettingsSection(
    settings: DesktopSettings,
    isNotificationServiceEnabled: () -> Boolean,
    isOverlayPermissionGranted: () -> Boolean,
    onShowNotificationPermissionDialog: () -> Unit,
    onShowOverlayPermissionDialog: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.floating_notifications))
            Text(stringResource(R.string.floating_notifications_desc), style = MaterialTheme.typography.bodySmall)
        }
        var fNotif by remember { mutableStateOf(settings.enableFloatingNotifications) }
        Switch(checked = fNotif, onCheckedChange = { fNotif = it; settings.enableFloatingNotifications = it })
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // ====== 4. 通知与系统 / Notifications & System ======
    Text(stringResource(R.string.system_settings), style = MaterialTheme.typography.titleLarge)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(stringResource(R.string.basic_mode))
            Text(text = stringResource(R.string.basic_mode_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        var bMode by remember { mutableStateOf(settings.isBasicMode) }
        Switch(checked = bMode, onCheckedChange = { bMode = it; settings.isBasicMode = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.desktop_notifications))
            Text(text = stringResource(R.string.desktop_notifications_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        var enableNotif by remember { mutableStateOf(settings.enableDesktopNotifications) }
        Switch(checked = enableNotif, onCheckedChange = { 
            if (it) {
                if (!isNotificationServiceEnabled()) onShowNotificationPermissionDialog()
                else if (!isOverlayPermissionGranted()) onShowOverlayPermissionDialog()
                else { enableNotif = true; settings.enableDesktopNotifications = true }
            } else { enableNotif = false; settings.enableDesktopNotifications = false }
        })
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.prevent_sleep))
            Text(text = stringResource(R.string.prevent_sleep_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        var preventS by remember { mutableStateOf(settings.preventSleep) }
        Switch(checked = preventS, onCheckedChange = { preventS = it; settings.preventSleep = it })
    }
}

package com.elderdesktop.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo
import com.elderdesktop.settings.sections.AppContactsSettingsSection
import com.elderdesktop.settings.sections.AppearanceSettingsSection
import com.elderdesktop.settings.sections.FeatureSettingsSection
import com.elderdesktop.settings.sections.LayoutSettingsSection
import com.elderdesktop.settings.sections.SafetySystemSettingsSection
import com.elderdesktop.settings.sections.SecurityWeatherSettingsSection
import com.elderdesktop.ui.EditContactDialog
import com.elderdesktop.ui.UnlockDialog
import com.elderdesktop.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { DesktopSettings(context) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
    }

    var usePasscode by remember { mutableStateOf(settings.usePasscode) }
    var passcode by remember { mutableStateOf(settings.passcode) }
    var showPasscodeDialog by remember { mutableStateOf(false) }

    var speedDialUpdateTrigger by remember { mutableIntStateOf(0) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    var showWhitelistDialog by remember { mutableStateOf(false) }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedWhitelist by remember { mutableStateOf(settings.notificationWhitelist) }

    var showAboutPage by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showAccessibilityPermissionDialog by remember { mutableStateOf(false) }
    var showUnlockForAccidentalTouch by remember { mutableStateOf(false) }

    fun isNotificationServiceEnabled(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    fun isOverlayPermissionGranted(): Boolean = Settings.canDrawOverlays(context)

    if (showAboutPage) {
        AlertDialog(
            onDismissRequest = { showAboutPage = false },
            title = { Text(stringResource(R.string.about)) },
            text = { AboutPage() },
            confirmButton = { Button(onClick = { showAboutPage = false }) { Text(stringResource(R.string.back)) } }
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text(stringResource(R.string.privacy_policy)) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = androidx.core.text.HtmlCompat.fromHtml(stringResource(R.string.privacy_policy_content), androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = { Button(onClick = { showPrivacyPolicy = false }) { Text(stringResource(R.string.back)) } }
        )
    }

    LaunchedEffect(showWhitelistDialog) {
        if (showWhitelistDialog && allApps.isEmpty()) {
            allApps = withContext(Dispatchers.IO) { AppUtils.getLaunchableApps(context) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ====== 1. 显示与布局 / Display & Layout ======
        LayoutSettingsSection(settings = settings, onRefresh = { speedDialUpdateTrigger++ })

        AppearanceSettingsSection(settings = settings)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 2. Home Screen Customization ======
        AppContactsSettingsSection(
            context = context,
            settings = settings,
            onRefresh = { speedDialUpdateTrigger++ },
            onEditContact = { index ->
                editingIndex = index
                showEditContactDialog = true
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 3. 功能 / Features ======
        FeatureSettingsSection(
            context = context,
            settings = settings,
            onShowUnlockForAccidentalTouch = { showUnlockForAccidentalTouch = true }
        )

        SafetySystemSettingsSection(
            settings = settings,
            isNotificationServiceEnabled = { isNotificationServiceEnabled() },
            isOverlayPermissionGranted = { isOverlayPermissionGranted() },
            onShowNotificationPermissionDialog = { showNotificationPermissionDialog = true },
            onShowOverlayPermissionDialog = { showOverlayPermissionDialog = true }
        )

        SecurityWeatherSettingsSection(
            settings = settings,
            usePasscode = usePasscode,
            passcode = passcode,
            onShowPasscodeDialog = { showPasscodeDialog = true },
            onShowPrivacyPolicy = { showPrivacyPolicy = true },
            onRefresh = { speedDialUpdateTrigger++ }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ====== 7. 其他 / Others ======
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = if (MaterialTheme.colorScheme.surface == Color.Black) Color.White else Color(0xFFE67E22))) {
            Text(stringResource(R.string.system_settings))
        }
        Button(onClick = { showAboutPage = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
            Text(stringResource(R.string.about))
        }
    }

    // Dialogs
    if (showPasscodeDialog) PasscodeDialog(onDismiss = { showPasscodeDialog = false }, onSave = { passcode = it; settings.passcode = it; usePasscode = true; settings.usePasscode = true; showPasscodeDialog = false })
    if (showEditContactDialog) EditContactDialog(index = editingIndex, settings = settings, onDismiss = { showEditContactDialog = false }, onSaved = { speedDialUpdateTrigger++ })
    if (showNotificationPermissionDialog) AlertDialog(onDismissRequest = { showNotificationPermissionDialog = false }, title = { Text(stringResource(R.string.permission_required)) }, confirmButton = { Button(onClick = { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); showNotificationPermissionDialog = false }) { Text(stringResource(R.string.grant)) } })
    if (showOverlayPermissionDialog) AlertDialog(onDismissRequest = { showOverlayPermissionDialog = false }, title = { Text(stringResource(R.string.overlay_required)) }, confirmButton = { Button(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())); showOverlayPermissionDialog = false }) { Text(stringResource(R.string.grant)) } })
    if (showAccessibilityPermissionDialog) AlertDialog(onDismissRequest = { showAccessibilityPermissionDialog = false }, title = { Text(stringResource(R.string.accessibility_required)) }, text = { Text(stringResource(R.string.accidental_touch_prevention_desc)) }, confirmButton = { Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); showAccessibilityPermissionDialog = false }) { Text(stringResource(R.string.grant)) } })
    if (showUnlockForAccidentalTouch) UnlockDialog(settings = settings, pendingApp = null, pendingSpeedDialIndex = -1, onUnlock = { _, _ -> context.startActivity(Intent(context, com.elderdesktop.launcher.AccidentalTouchSettingsActivity::class.java)) }, onDismiss = { showUnlockForAccidentalTouch = false })
    if (showWhitelistDialog) AppSelectionDialog(allApps = allApps, selectedApps = selectedWhitelist, onSelectionChanged = { selectedWhitelist = it }, onDismiss = { showWhitelistDialog = false }, onSave = { settings.notificationWhitelist = selectedWhitelist; showWhitelistDialog = false })
}

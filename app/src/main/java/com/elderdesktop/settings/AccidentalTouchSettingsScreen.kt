package com.elderdesktop.settings

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.launcher.AccidentalTouchService

@Composable
fun AccidentalTouchSettingsScreen() {
    val context = LocalContext.current
    val settings = remember { DesktopSettings(context) }
    val scrollState = rememberScrollState()

    var accidentalTouch by remember { mutableStateOf(settings.preventAccidentalTouch) }
    var edgeTouch by remember { mutableStateOf(settings.preventEdgeTouch) }
    var adTouch by remember { mutableStateOf(settings.preventAdTouch) }
    var unknownInstall by remember { mutableStateOf(settings.preventUnknownInstall) }
    
    var intercept400 by remember { mutableStateOf(settings.intercept400Calls) }
    var interceptOverseas by remember { mutableStateOf(settings.interceptOverseasCalls) }
    var interceptSpam by remember { mutableStateOf(settings.interceptSpamCalls) }

    var showAccessibilityPermissionDialog by remember { mutableStateOf(false) }

    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(context, AccidentalTouchService::class.java)
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedComponentName.flattenToString()) == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.accidental_touch_prevention),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Switch(
                        checked = accidentalTouch,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (!isAccessibilityServiceEnabled()) {
                                    showAccessibilityPermissionDialog = true
                                } else {
                                    accidentalTouch = true
                                    settings.preventAccidentalTouch = true
                                    context.startService(Intent(context, AccidentalTouchService::class.java))
                                }
                            } else {
                                accidentalTouch = false
                                settings.preventAccidentalTouch = false
                                context.startService(Intent(context, AccidentalTouchService::class.java))
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Edge Protection
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.edge_protection), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.edge_protection_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = edgeTouch, onCheckedChange = { edgeTouch = it; settings.preventEdgeTouch = it })
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Ad Protection
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ad_protection), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.ad_protection_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = adTouch, onCheckedChange = { adTouch = it; settings.preventAdTouch = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Install Prevention
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.install_protection), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.install_protection_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = unknownInstall, onCheckedChange = { unknownInstall = it; settings.preventUnknownInstall = it })
                }

                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.spam_call_interception), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.intercept_400_calls))
                    Switch(checked = intercept400, onCheckedChange = { intercept400 = it; settings.intercept400Calls = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.intercept_overseas_calls))
                    Switch(checked = interceptOverseas, onCheckedChange = { interceptOverseas = it; settings.interceptOverseasCalls = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.intercept_spam_calls))
                    Switch(checked = interceptSpam, onCheckedChange = { interceptSpam = it; settings.interceptSpamCalls = it })
                }
            }
        }

        Text(
            text = stringResource(R.string.accidental_touch_privacy_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    if (showAccessibilityPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_required)) },
            text = { Text(stringResource(R.string.accidental_touch_prevention_desc)) },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    showAccessibilityPermissionDialog = false
                }) {
                    Text(stringResource(R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

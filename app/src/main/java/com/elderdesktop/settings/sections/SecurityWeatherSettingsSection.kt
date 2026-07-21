package com.elderdesktop.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R

@Composable
fun SecurityWeatherSettingsSection(
    settings: DesktopSettings,
    usePasscode: Boolean,
    passcode: String,
    onShowPasscodeDialog: () -> Unit,
    onShowPrivacyPolicy: () -> Unit,
    onRefresh: () -> Unit
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // ====== 5. 安全与隐私 / Security & Privacy ======
    Text(stringResource(R.string.peace_of_mind_lock), style = MaterialTheme.typography.titleLarge)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.peace_of_mind_lock))
        var lockEnabled by remember { mutableStateOf(usePasscode) }
        Switch(checked = lockEnabled, onCheckedChange = { if (it && passcode.isEmpty()) onShowPasscodeDialog() else { lockEnabled = it; settings.usePasscode = it } })
    }
    if (usePasscode) {
        TextButton(onClick = onShowPasscodeDialog) { Text(stringResource(R.string.change_passcode, passcode)) }
    }
    TextButton(onClick = onShowPrivacyPolicy, modifier = Modifier) {
        Text(stringResource(R.string.privacy_policy))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // ====== 6. 天气 / Weather ======
    Text(stringResource(R.string.weather), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.weather_provider), style = MaterialTheme.typography.bodyMedium)
    var weatherP by remember { mutableStateOf(settings.weatherProvider) }
    Column(modifier = Modifier.padding(start = 16.dp)) {
        listOf("open-meteo" to "Open-Meteo (Free)", "qweather" to "QWeather (HeWeather)", "openweather" to "OpenWeatherMap").forEach { (id, name) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = weatherP == id, onClick = { weatherP = id; settings.weatherProvider = id })
                Text(name, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (weatherP != "open-meteo") {
        var weatherKey by remember { mutableStateOf(settings.weatherApiKey) }
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

    val manualLocs = settings.manualWeatherLocations
    if (manualLocs.isNotEmpty()) {
        manualLocs.forEach { loc ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(loc, modifier = Modifier.padding(start = 8.dp))
                    IconButton(onClick = { settings.removeWeatherLocation(loc); onRefresh() }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

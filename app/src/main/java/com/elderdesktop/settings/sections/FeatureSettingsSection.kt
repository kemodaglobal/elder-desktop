package com.elderdesktop.settings.sections

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
fun FeatureSettingsSection(
    context: Context,
    settings: DesktopSettings,
    onShowUnlockForAccidentalTouch: () -> Unit
) {
    Text(stringResource(R.string.features), style = MaterialTheme.typography.titleLarge)
    
    var showVoice by remember { mutableStateOf(settings.showVoiceAssistant) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.voice_assistant_button))
        Switch(checked = showVoice, onCheckedChange = { showVoice = it; settings.showVoiceAssistant = it })
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            if (settings.usePasscode) onShowUnlockForAccidentalTouch()
            else context.startActivity(Intent(context, com.elderdesktop.launcher.AccidentalTouchSettingsActivity::class.java))
        }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.accidental_touch_settings_title), style = MaterialTheme.typography.bodyLarge)
            Text(text = stringResource(R.string.accidental_touch_prevention_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }

    var voiceAnnouncements by remember { mutableStateOf(settings.voiceAnnouncements) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.voice_announcements))
        Switch(checked = voiceAnnouncements, onCheckedChange = { voiceAnnouncements = it; settings.voiceAnnouncements = it })
    }

    // AI Chat Section
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ai_chat), style = MaterialTheme.typography.bodyLarge)
                Text(text = stringResource(R.string.ai_chat_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            var enableAI by remember { mutableStateOf(settings.enableDeepSeek) }
            Switch(checked = enableAI, onCheckedChange = { enableAI = it; settings.enableDeepSeek = it })
        }

        if (settings.enableDeepSeek) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.ai_provider), style = MaterialTheme.typography.bodyMedium)
            var currentProvider by remember { mutableStateOf(settings.aiProvider) }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                val providers = listOf(
                    "deepseek" to R.string.ai_provider_deepseek,
                    "openai" to R.string.ai_provider_openai,
                    "google" to R.string.ai_provider_google
                )
                providers.forEach { (id, nameRes) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentProvider == id, onClick = { currentProvider = id; settings.aiProvider = id })
                        Text(stringResource(nameRes), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            when (currentProvider) {
                "deepseek" -> {
                    var apiKey by remember { mutableStateOf(settings.deepSeekApiKey) }
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it; settings.deepSeekApiKey = it }, label = { Text(stringResource(R.string.deepseek_api_key)) }, placeholder = { Text(stringResource(R.string.enter_api_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "openai" -> {
                    var apiKey by remember { mutableStateOf(settings.openAiApiKey) }
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it; settings.openAiApiKey = it }, label = { Text(stringResource(R.string.openai_api_key)) }, placeholder = { Text(stringResource(R.string.enter_api_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "google" -> {
                    var apiKey by remember { mutableStateOf(settings.googleAiApiKey) }
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it; settings.googleAiApiKey = it }, label = { Text(stringResource(R.string.google_ai_api_key)) }, placeholder = { Text(stringResource(R.string.enter_api_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
        }
    }
}

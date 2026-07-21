package com.elderdesktop.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
fun AppearanceSettingsSection(
    settings: DesktopSettings
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.high_contrast))
        var hContrast by remember { mutableStateOf(settings.highContrastMode) }
        Switch(checked = hContrast, onCheckedChange = { hContrast = it; settings.highContrastMode = it })
    }

    // UI Style Selection
    Column {
        Text(stringResource(R.string.ui_style_selection), style = MaterialTheme.typography.bodyLarge)
        val styles = listOf(
            "modern" to R.string.ui_style_modern,
            "holo" to R.string.ui_style_holo
        )
        var selectedStyle by remember { mutableStateOf(settings.uiStyle) }
        styles.forEach { (id, labelRes) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedStyle == id,
                    onClick = {
                        selectedStyle = id
                        settings.uiStyle = id
                    }
                )
                Text(stringResource(labelRes), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    // Theme Selection
    Column {
        Text(stringResource(R.string.theme_selection), style = MaterialTheme.typography.bodyLarge)
        val themes = listOf("classic" to R.string.theme_classic, "emerald" to R.string.theme_emerald, "rose" to R.string.theme_rose, "orange" to R.string.theme_orange, "high_contrast" to R.string.theme_high_contrast)
        var selectedTheme by remember { mutableStateOf(settings.themeChoice) }
        themes.forEach { (id, labelRes) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedTheme == id, onClick = { selectedTheme = id; settings.themeChoice = id })
                Text(stringResource(labelRes), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    // Font Selection
    Column {
        Text(stringResource(R.string.font_selection), style = MaterialTheme.typography.bodyLarge)
        val fonts = listOf("default" to R.string.font_default, "serif" to R.string.font_serif, "monospace" to R.string.font_monospace)
        var selectedFont by remember { mutableStateOf(settings.fontChoice) }
        fonts.forEach { (id, labelRes) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedFont == id, onClick = { selectedFont = id; settings.fontChoice = id })
                Text(stringResource(labelRes), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

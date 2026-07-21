package com.elderdesktop.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R

@Composable
fun LayoutSettingsSection(
    settings: DesktopSettings,
    onRefresh: () -> Unit
) {
    Text(stringResource(R.string.layout), style = MaterialTheme.typography.titleLarge)
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = settings.useAutoLayout, onClick = { settings.useAutoLayout = true; onRefresh() })
        Text(stringResource(R.string.layout_auto), modifier = Modifier.padding(start = 8.dp))
    }

    val configuration = LocalConfiguration.current
    val sw = configuration.smallestScreenWidthDp
    
    if (sw < 600) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !settings.useAutoLayout && settings.is2x3, onClick = { settings.useAutoLayout = false; settings.use2x3(); onRefresh() })
            Text(stringResource(R.string.layout_2x3), modifier = Modifier.padding(start = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !settings.useAutoLayout && settings.is3x4, onClick = { settings.useAutoLayout = false; settings.use3x4(); onRefresh() })
            Text(stringResource(R.string.layout_3x4), modifier = Modifier.padding(start = 8.dp))
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !settings.useAutoLayout && settings.is4x3, onClick = { settings.useAutoLayout = false; settings.use4x3(); onRefresh() })
            Text(stringResource(R.string.layout_4x3), modifier = Modifier.padding(start = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !settings.useAutoLayout && settings.is6x4, onClick = { settings.useAutoLayout = false; settings.use6x4(); onRefresh() })
            Text(stringResource(R.string.layout_6x4), modifier = Modifier.padding(start = 8.dp))
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = !settings.useAutoLayout && settings.is1x2, onClick = { settings.useAutoLayout = false; settings.use1x2(); onRefresh() })
        Text(stringResource(R.string.layout_1x2), modifier = Modifier.padding(start = 8.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = !settings.useAutoLayout && settings.isSingleColScrolling, onClick = { settings.useAutoLayout = false; settings.useSingleColumnScrolling(); onRefresh() })
        Text(stringResource(R.string.layout_scrolling_single), modifier = Modifier.padding(start = 8.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = !settings.useAutoLayout && settings.isDoubleColScrolling, onClick = { settings.useAutoLayout = false; settings.useDoubleColumnScrolling(); onRefresh() })
        Text(stringResource(R.string.layout_scrolling_double), modifier = Modifier.padding(start = 8.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = !settings.useAutoLayout && settings.is3x2, onClick = { settings.useAutoLayout = false; settings.use3x2(); onRefresh() })
        Text(stringResource(R.string.layout_3x2), modifier = Modifier.padding(start = 8.dp))
    }

    Column {
        Text(stringResource(R.string.font_size))
        var fSize by remember { mutableFloatStateOf(settings.fontSizeMultiplier) }
        Slider(value = fSize, onValueChange = { fSize = it }, onValueChangeFinished = { settings.fontSizeMultiplier = fSize }, valueRange = 0.8f..1.5f, steps = 5)
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
        val shapeLabels = listOf(R.string.shape_rounded, R.string.shape_circle, R.string.shape_square, R.string.shape_native)
        var selectedShape by remember { mutableStateOf(settings.iconShape) }
        shapes.forEachIndexed { index, shape ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedShape == shape, onClick = { selectedShape = shape; settings.iconShape = shape; onRefresh() })
                Text(stringResource(shapeLabels[index]), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

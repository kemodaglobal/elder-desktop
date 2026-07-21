package com.elderdesktop.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo
import com.elderdesktop.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EditDesktopScreen() {
    val context = LocalContext.current
    val settings = remember { DesktopSettings(context) }
    var layoutItems by remember { mutableStateOf(settings.layoutOrder) }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        allApps = withContext(Dispatchers.IO) { AppUtils.getLaunchableApps(context) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.drag_to_reorder), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(layoutItems) { index, item ->
                val label: String
                val icon: android.graphics.drawable.Drawable?
                
                if (item == "widget:clock") {
                    label = stringResource(R.string.weather) + " " + stringResource(R.string.clock)
                    icon = null
                } else {
                    val pkg = item.removePrefix("app:")
                    val app = allApps.find { it.packageName == pkg }
                    label = app?.label ?: pkg
                    icon = app?.icon
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Image(bitmap = icon.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
                        } else {
                            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        
                        IconButton(onClick = {
                            if (index > 0) {
                                val newList = layoutItems.toMutableList()
                                val temp = newList[index]
                                newList[index] = newList[index - 1]
                                newList[index - 1] = temp
                                layoutItems = newList
                                settings.layoutOrder = newList
                            }
                        }, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                        }
                        IconButton(onClick = {
                            if (index < layoutItems.size - 1) {
                                val newList = layoutItems.toMutableList()
                                val temp = newList[index]
                                newList[index] = newList[index + 1]
                                newList[index + 1] = temp
                                layoutItems = newList
                                settings.layoutOrder = newList
                            }
                        }, enabled = index < layoutItems.size - 1) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
                        }
                        IconButton(onClick = {
                            val newList = layoutItems.toMutableList()
                            newList.removeAt(index)
                            layoutItems = newList
                            settings.layoutOrder = newList
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showAppPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_item))
        }
    }

    if (showAppPicker) {
        val currentPkgs = layoutItems.filter { it.startsWith("app:") }.map { it.removePrefix("app:") }.toSet()
        val availableApps = allApps.filter { it.packageName !in currentPkgs }
        
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text(stringResource(R.string.select_apps)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (!layoutItems.contains("widget:clock")) {
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.weather) + " " + stringResource(R.string.clock)) },
                                modifier = Modifier.clickable {
                                    val newList = layoutItems.toMutableList()
                                    newList.add("widget:clock")
                                    layoutItems = newList
                                    settings.layoutOrder = newList
                                    showAppPicker = false
                                }
                            )
                        }
                    }
                    itemsIndexed(availableApps) { _, app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = { Image(bitmap = app.icon.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp)) },
                            modifier = Modifier.clickable {
                                val newList = layoutItems.toMutableList()
                                newList.add("app:${app.packageName}")
                                layoutItems = newList
                                settings.layoutOrder = newList
                                showAppPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAppPicker = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

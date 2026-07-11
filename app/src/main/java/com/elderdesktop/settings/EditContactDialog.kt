package com.elderdesktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R

@Composable
fun EditContactDialog(
    index: Int,
    settings: DesktopSettings,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val existingContact = settings.getSpeedDial(index)
    var name by remember { mutableStateOf(existingContact?.first ?: "") }
    var number by remember { mutableStateOf(existingContact?.second ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingContact == null) stringResource(R.string.add_contact)
                else stringResource(R.string.edit_contact)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && number.isNotEmpty()) {
                    settings.setSpeedDial(index, name, number)
                    onSaved()
                    onDismiss()
                }
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
package com.elderdesktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo
import com.elderdesktop.DesktopSettings

@Composable
fun UnlockDialog(
    settings: DesktopSettings,
    pendingApp: AppInfo?,
    pendingSpeedDialIndex: Int,
    onUnlock: (AppInfo?, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputPasscode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.peace_of_mind_lock)) },
        text = {
            Column {
                Text(stringResource(R.string.passcode_prompt))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputPasscode,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 4) {
                            inputPasscode = it
                            isError = false
                        }
                    },
                    label = { Text(stringResource(R.string.passcode)) },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) Text(stringResource(R.string.incorrect_passcode))
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (inputPasscode == settings.passcode) {
                    onUnlock(pendingApp, pendingSpeedDialIndex)
                    onDismiss()
                } else {
                    isError = true
                }
            }) {
                Text(stringResource(R.string.unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AddContactDialog(
    editingSpeedDialIndex: Int,
    settings: DesktopSettings,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_contact)) },
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
                    settings.setSpeedDial(editingSpeedDialIndex, name, number)
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
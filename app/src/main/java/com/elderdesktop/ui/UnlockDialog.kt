package com.elderdesktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo

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

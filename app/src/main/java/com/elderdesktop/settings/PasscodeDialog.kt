package com.elderdesktop.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.elderdesktop.R

@Composable
fun PasscodeDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tempPasscode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_passcode)) },
        text = {
            OutlinedTextField(
                value = tempPasscode,
                onValueChange = { 
                    if ((it.all { char -> char.isDigit() }) && (it.length <= 4)) {
                        tempPasscode = it
                    }
                },
                label = { Text(stringResource(R.string.enter_4_digits)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tempPasscode.length == 4) {
                        onSave(tempPasscode)
                    }
                }
            ) {
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
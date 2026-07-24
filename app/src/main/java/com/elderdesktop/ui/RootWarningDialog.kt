package com.elderdesktop.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R

@Composable
fun RootWarningDialog(
    settings: DesktopSettings,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent accidental dismissal */ },
        title = {
            Text(text = stringResource(id = R.string.root_warning_title))
        },
        text = {
            Text(text = stringResource(id = R.string.root_warning_message))
        },
        confirmButton = {
            Button(
                onClick = {
                    settings.rootWarningAcknowledged = true
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.root_warning_button))
            }
        }
    )
}

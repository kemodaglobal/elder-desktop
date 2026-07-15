package com.elderdesktop.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.text.HtmlCompat
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo

@Composable
fun PrivacyAgreementDialog(
    settings: DesktopSettings,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal without action */ },
        title = { Text(stringResource(R.string.privacy_agreement_title)) },
        text = {
            Column {
                Text(stringResource(R.string.privacy_agreement_desc))
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = HtmlCompat.fromHtml(
                            stringResource(R.string.privacy_policy_content),
                            HtmlCompat.FROM_HTML_MODE_COMPACT
                        ).toString(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                settings.privacyAccepted = true
                onAccept()
            }) {
                Text(stringResource(R.string.agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.decline))
            }
        }
    )
}

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
fun LocationSelectionDialog(
    settings: DesktopSettings,
    onDismiss: () -> Unit,
    onLocationAdded: () -> Unit
) {
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_location)) },
        text = {
            Column {
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it },
                    label = { Text(stringResource(R.string.province)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.city)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text(stringResource(R.string.district)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (province.isNotEmpty() && city.isNotEmpty()) {
                    val fullLocation = "$province $city $district".trim()
                    settings.addWeatherLocation(fullLocation)
                    onLocationAdded()
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

@Composable
fun AddContactDialog(
    editingSpeedDialIndex: Int,
    settings: DesktopSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Fallback for URIs that don't support persistable permissions
                }
                photoUri = uri.toString()
            }
        }
    )

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            uri?.let { contactUri ->
                try {
                    val projection = arrayOf(
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts._ID
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            name = if (nameIndex >= 0) cursor.getString(nameIndex) else ""
                            val id = if (idIndex >= 0) cursor.getString(idIndex) else ""

                            if (id.isNotEmpty()) {
                                val phoneProjection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                val phoneSelection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
                                context.contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    phoneProjection,
                                    phoneSelection,
                                    arrayOf(id),
                                    null
                                )?.use { phoneCursor ->
                                    if (phoneCursor.moveToFirst()) {
                                        val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        number = if (numberIndex >= 0) phoneCursor.getString(numberIndex) else ""
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AddContactDialog", "Error querying contact", e)
                    Toast.makeText(context, R.string.weather_error_network, Toast.LENGTH_SHORT).show() // Using a generic error string for now or better add a specific one
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                contactPickerLauncher.launch(null)
            } else {
                Toast.makeText(context, R.string.permission_required, Toast.LENGTH_SHORT).show()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_contact)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri!!.toUri(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(id = R.string.add_photo),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)) {
                            PackageManager.PERMISSION_GRANTED -> {
                                contactPickerLauncher.launch(null)
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.import_contacts))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && number.isNotEmpty()) {
                    settings.setSpeedDial(editingSpeedDialIndex, name, number, photoUri)
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

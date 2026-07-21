package com.elderdesktop.settings.sections

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.rememberAsyncImagePainter
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R

@Composable
fun AppContactsSettingsSection(
    context: Context,
    settings: DesktopSettings,
    onRefresh: () -> Unit,
    onEditContact: (Int) -> Unit
) {
    // ====== 2. Home Screen Customization ======
    Text(stringResource(R.string.edit_home_screen), style = MaterialTheme.typography.titleLarge)
    
    Button(
        onClick = { context.startActivity(Intent(context, com.elderdesktop.launcher.EditDesktopActivity::class.java)) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.edit_home_screen))
    }

    Text(stringResource(R.string.add_contact), style = MaterialTheme.typography.titleLarge)
    val totalSpeedDials = (settings.layoutRows - 1) * settings.layoutCols
    for (i in 0 until totalSpeedDials) {
        val contact = settings.getSpeedDial(i)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (contact?.third != null) {
                        Image(painter = rememberAsyncImagePainter(contact.third!!.toUri()), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(text = contact?.first ?: stringResource(R.string.add_contact), style = MaterialTheme.typography.bodyLarge)
                        if (contact != null) Text(text = contact.second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                Row {
                    IconButton(onClick = { onEditContact(i) }) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_contact)) }
                    if (contact != null) IconButton(onClick = { settings.clearSpeedDial(i); onRefresh() }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

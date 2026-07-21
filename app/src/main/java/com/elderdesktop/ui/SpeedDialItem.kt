package com.elderdesktop.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.ui.theme.HoloBlue

@Composable
fun SpeedDialItem(
    index: Int,
    settings: DesktopSettings,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 22.sp,
    iconSizeMultiplier: Float = 1.0f,
    iconShape: String = "rounded",
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null,
    onClick: () -> Unit
) {
    val contact = settings.getSpeedDial(index)
    val isHighContrast = MaterialTheme.colorScheme.surface == Color.Black
    val isHolo = MaterialTheme.colorScheme.primary == HoloBlue
    
    val backgroundColor = if (isHighContrast || isHolo) MaterialTheme.colorScheme.surface
                         else if (contact == null) Color(0xFF1A5F7A).copy(alpha = 0.9f)
                         else Color(0xFF2ECC71).copy(alpha = 0.9f)
                         
    val contentColor = if (isHighContrast || isHolo) MaterialTheme.colorScheme.onSurface
                      else getContrastColor(backgroundColor)
                      
    val shape = getIconShape(iconShape)
    val iconSize = 80.dp * iconSizeMultiplier

    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isHighContrast || isHolo) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                else Modifier
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (contact == null) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(shape),
                    tint = contentColor.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.add_contact),
                    fontSize = 20.sp * (labelSize.value / 22f),
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            } else {
                val photoUri = contact.third
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri.toUri(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(shape),
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(shape),
                        tint = contentColor
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = contact.first,
                    fontSize = labelSize,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

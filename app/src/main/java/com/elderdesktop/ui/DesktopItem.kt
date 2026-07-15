package com.elderdesktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo

@Composable
fun DesktopItem(
    app: AppInfo,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 22.sp,
    iconSizeMultiplier: Float = 1.0f,
    iconShape: String = "rounded",
    colorFilter: ColorFilter? = null,
    onClick: () -> Unit
) {
    val shape = getIconShape(iconShape)
    val iconSize = 80.dp * iconSizeMultiplier
    val isHighContrast = MaterialTheme.colorScheme.surface == Color.Black

    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isHighContrast) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighContrast) MaterialTheme.colorScheme.surface 
                             else app.backgroundColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = app.icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .clip(shape),
                colorFilter = colorFilter,
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = app.label,
                fontSize = labelSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SpeedDialItem(
    index: Int,
    settings: DesktopSettings,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 22.sp,
    iconSizeMultiplier: Float = 1.0f,
    iconShape: String = "rounded",
    colorFilter: ColorFilter? = null,
    onClick: () -> Unit
) {
    val contact = settings.getSpeedDial(index)
    val isHighContrast = MaterialTheme.colorScheme.surface == Color.Black
    val backgroundColor = if (isHighContrast) MaterialTheme.colorScheme.surface
                         else if (contact == null) Color(0xFF1A5F7A) 
                         else Color(0xFF2ECC71)
    val shape = getIconShape(iconShape)
    val iconSize = 80.dp * iconSizeMultiplier

    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isHighContrast) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.9f)),
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
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.add_contact),
                    fontSize = 20.sp * (labelSize.value / 22f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
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
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = contact.first,
                    fontSize = labelSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getIconShape(shape: String): Shape {
    return when (shape) {
        "circle" -> CircleShape
        "square" -> RectangleShape
        "native" -> RectangleShape // Native often means unclipped or system-clipped
        else -> RoundedCornerShape(16.dp)
    }
}

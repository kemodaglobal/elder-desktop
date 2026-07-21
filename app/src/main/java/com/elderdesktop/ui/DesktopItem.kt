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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.elderdesktop.model.AppInfo
import com.elderdesktop.ui.theme.HoloBlue

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
    val isHolo = MaterialTheme.colorScheme.primary == HoloBlue

    val backgroundColor = if (isHighContrast || isHolo) MaterialTheme.colorScheme.surface 
                         else app.backgroundColor.copy(alpha = 0.9f)
                         
    val contentColor = if (isHighContrast || isHolo) MaterialTheme.colorScheme.onSurface
                      else getContrastColor(backgroundColor)

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
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

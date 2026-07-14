package com.elderdesktop.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderdesktop.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("DefaultLocale")
@Composable
fun ClockWidget(
    weatherText: String,
    isWeatherAlert: Boolean,
    locationCity: String = "",
    currentTemperature: String = "",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1.0f,
    onClick: () -> Unit = {},
    onAddLocation: () -> Unit = {}
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000.milliseconds)
        }
    }

    val hour24 = currentTime.get(Calendar.HOUR_OF_DAY)
    val hour12 = currentTime.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = String.format("%02d", currentTime.get(Calendar.MINUTE))

    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val marker = getTimeOfDayMarker(context, locale, hour24)

    val dateString = SimpleDateFormat("M/d/yyyy EEEE", locale).format(currentTime.time)

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A5F7A).copy(alpha = 0.9f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (weatherText.isEmpty() && currentTemperature.isEmpty()) {
                IconButton(
                    onClick = onAddLocation,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_location),
                        tint = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (locationCity.isNotEmpty()) {
                        Text(
                            text = locationCity,
                            fontSize = 14.sp * fontSizeMultiplier,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Text(
                        text = "$marker $hour12:$minute",
                        fontSize = 28.sp * fontSizeMultiplier,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = dateString,
                        fontSize = 12.sp * fontSizeMultiplier,
                        color = Color.White
                    )
                    if (currentTemperature.isNotEmpty() || weatherText.isNotEmpty()) {
                        val weatherDisplay = listOfNotNull(
                            currentTemperature.ifEmpty { null },
                            weatherText.ifEmpty { null }
                        ).joinToString(" ")
                        Text(
                            text = weatherDisplay,
                            fontSize = (if (isWeatherAlert) 18.sp else 14.sp) * fontSizeMultiplier,
                            fontWeight = if (isWeatherAlert) FontWeight.Bold else FontWeight.Normal,
                            color = if (isWeatherAlert) Color.Red else Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.Yellow)
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun SimpleClockWidget(
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1.0f
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000.milliseconds)
        }
    }

    val hour24 = currentTime.get(Calendar.HOUR_OF_DAY)
    val hour12 = currentTime.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = String.format("%02d", currentTime.get(Calendar.MINUTE))

    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val marker = getTimeOfDayMarker(context, locale, hour24)

    val dateString = SimpleDateFormat("M/d/yyyy EEEE", locale).format(currentTime.time)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A5F7A).copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$marker $hour12:$minute",
                fontSize = 40.sp * fontSizeMultiplier,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = dateString,
                fontSize = 18.sp * fontSizeMultiplier,
                color = Color.White
            )
        }
    }
}

private fun getTimeOfDayMarker(context: android.content.Context, locale: Locale, hour: Int): String {
    return if (locale.language == "zh") {
        when (hour) {
            in 0..1 -> context.getString(R.string.time_midnight)
            in 2..5 -> context.getString(R.string.time_dawn)
            in 6..11 -> context.getString(R.string.time_morning)
            in 12..13 -> context.getString(R.string.time_noon)
            in 14..17 -> context.getString(R.string.time_afternoon)
            else -> context.getString(R.string.time_evening)
        }
    } else {
        if (hour < 12) "AM" else "PM"
    }
}
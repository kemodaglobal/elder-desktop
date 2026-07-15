package com.elderdesktop.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    weatherCode: Int = 800,
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
    val isHighContrast = MaterialTheme.colorScheme.surface == Color.Black

    val isDay = hour24 in 6..18
    val weatherType = com.elderdesktop.util.WeatherUtils.getWeatherType(weatherCode)

    val widgetBackgroundColor = if (isHighContrast) MaterialTheme.colorScheme.surface
    else when (weatherType) {
        com.elderdesktop.util.WeatherUtils.WeatherType.CLEAR -> if (isDay) Color(0xFF87CEEB) else Color(0xFF1A237E)
        com.elderdesktop.util.WeatherUtils.WeatherType.CLOUDY,
        com.elderdesktop.util.WeatherUtils.WeatherType.RAIN,
        com.elderdesktop.util.WeatherUtils.WeatherType.SNOW,
        com.elderdesktop.util.WeatherUtils.WeatherType.ATMOSPHERE -> if (isDay) Color(0xFFBDBDBD) else Color(0xFF424242)
        else -> Color(0xFF1A5F7A)
    }

    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isHighContrast) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = widgetBackgroundColor.copy(alpha = 0.9f)
        )
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
                        tint = MaterialTheme.colorScheme.onSurface
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
                            fontWeight = if (isHighContrast) FontWeight.Black else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                    Text(
                        text = "$marker $hour12:$minute",
                        fontSize = 28.sp * fontSizeMultiplier,
                        fontWeight = if (isHighContrast) FontWeight.Black else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateString,
                        fontSize = 12.sp * fontSizeMultiplier,
                        fontWeight = if (isHighContrast) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentTemperature.isNotEmpty() || weatherText.isNotEmpty()) {
                        val weatherDisplay = listOfNotNull(
                            currentTemperature.ifEmpty { null },
                            weatherText.ifEmpty { null }
                        ).joinToString(" ")
                        Text(
                            text = weatherDisplay,
                            fontSize = (if (isWeatherAlert) 18.sp else 14.sp) * fontSizeMultiplier,
                            fontWeight = if (isWeatherAlert) FontWeight.Black else if (isHighContrast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isWeatherAlert) (if (isHighContrast) Color.Yellow else Color.Red) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isHighContrast) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White))
                    } else {
                        WeatherIcon(weatherType, isDay)
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherIcon(type: com.elderdesktop.util.WeatherUtils.WeatherType, isDay: Boolean) {
    when (type) {
        com.elderdesktop.util.WeatherUtils.WeatherType.CLEAR -> {
            if (isDay) {
                Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.Yellow)
            } else {
                Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.White)
            }
        }
        com.elderdesktop.util.WeatherUtils.WeatherType.CLOUDY -> {
            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.White)
        }
        com.elderdesktop.util.WeatherUtils.WeatherType.RAIN -> {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.DarkGray)
                Icon(Icons.Default.Grain, contentDescription = null, modifier = Modifier.size(30.dp).padding(top = 20.dp), tint = Color.Cyan)
            }
        }
        com.elderdesktop.util.WeatherUtils.WeatherType.SNOW -> {
            Icon(Icons.Default.Grain, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.White)
        }
        else -> {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Yellow))
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun SimpleClockWidget(
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1.0f,
    weatherCode: Int = 800
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
    val isHighContrast = MaterialTheme.colorScheme.surface == Color.Black

    val isDay = hour24 in 6..18
    val weatherType = com.elderdesktop.util.WeatherUtils.getWeatherType(weatherCode)

    val widgetBackgroundColor = if (isHighContrast) MaterialTheme.colorScheme.surface
    else when (weatherType) {
        com.elderdesktop.util.WeatherUtils.WeatherType.CLEAR -> if (isDay) Color(0xFF87CEEB) else Color(0xFF1A237E)
        com.elderdesktop.util.WeatherUtils.WeatherType.CLOUDY,
        com.elderdesktop.util.WeatherUtils.WeatherType.RAIN,
        com.elderdesktop.util.WeatherUtils.WeatherType.SNOW,
        com.elderdesktop.util.WeatherUtils.WeatherType.ATMOSPHERE -> if (isDay) Color(0xFFBDBDBD) else Color(0xFF424242)
        else -> Color(0xFF1A5F7A)
    }

    Card(
        modifier = modifier
            .then(
                if (isHighContrast) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = widgetBackgroundColor.copy(alpha = 0.9f)
        )
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
                fontWeight = if (isHighContrast) FontWeight.Black else FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dateString,
                fontSize = 18.sp * fontSizeMultiplier,
                fontWeight = if (isHighContrast) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
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
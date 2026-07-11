package com.elderdesktop.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("DefaultLocale")
@Composable
fun ClockWidget(
    weatherText: String,
    isWeatherAlert: Boolean,
    locationCity: String = "",
    currentTemperature: String = ""
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

    val locale = LocalLocale.current.platformLocale
    val marker = getTimeOfDayMarker(locale, hour24)
    
    val dateString = SimpleDateFormat("M/d/yyyy EEEE", locale).format(currentTime.time)
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = statusBarHeight + 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A5F7A).copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (locationCity.isNotEmpty()) {
                    Text(
                        text = locationCity,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Text(
                    text = "$marker $hour12:$minute",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = dateString,
                    fontSize = 14.sp,
                    color = Color.White
                )
                if (currentTemperature.isNotEmpty() || weatherText.isNotEmpty()) {
                    val weatherDisplay = listOfNotNull(currentTemperature.ifEmpty { null }, weatherText.ifEmpty { null })
                        .joinToString(" ")
                    Text(
                        text = weatherDisplay,
                        fontSize = if (isWeatherAlert) 22.sp else 18.sp,
                        fontWeight = if (isWeatherAlert) FontWeight.Bold else FontWeight.Normal,
                        color = if (isWeatherAlert) Color.Red else Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Yellow)
            )
        }
    }
}

private fun getTimeOfDayMarker(locale: Locale, hour: Int): String {
    return if (locale.language == "zh") {
        when (hour) {
            in 0..1 -> "半夜"
            in 2..5 -> "凌晨"
            in 6..11 -> "上午"
            in 12..13 -> "中午"
            in 14..17 -> "下午"
            else -> "晚上"
        }
    } else {
        if (hour < 12) "AM" else "PM"
    }
}

package com.elderdesktop.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderdesktop.model.WeatherType
import com.elderdesktop.ui.theme.HoloBlue
import com.elderdesktop.util.TimeUtils
import com.elderdesktop.util.WeatherUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

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
    val marker = TimeUtils.getTimeOfDayMarker(context, locale, hour24)

    val dateString = SimpleDateFormat("M/d/yyyy EEEE", locale).format(currentTime.time)
    val isHighContrast = MaterialTheme.colorScheme.surface == Color.Black
    val isHolo = MaterialTheme.colorScheme.primary == HoloBlue

    val isDay = hour24 in 6..18
    val weatherType = WeatherUtils.getWeatherType(weatherCode)

    val widgetBackgroundColor = if (isHighContrast || isHolo) MaterialTheme.colorScheme.surface
    else when (weatherType) {
        WeatherType.CLEAR -> if (isDay) Color(0xFF87CEEB) else Color(0xFF1A237E)
        WeatherType.CLOUDY,
        WeatherType.RAIN,
        WeatherType.SNOW,
        WeatherType.ATMOSPHERE -> if (isDay) Color(0xFFBDBDBD) else Color(0xFF424242)
        else -> Color(0xFF1A5F7A)
    }

    Card(
        modifier = modifier
            .then(
                if (isHighContrast || isHolo) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                else Modifier
            ),
        shape = MaterialTheme.shapes.medium,
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

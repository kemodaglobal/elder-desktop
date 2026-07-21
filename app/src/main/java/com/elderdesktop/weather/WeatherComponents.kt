package com.elderdesktop.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderdesktop.R
import com.elderdesktop.model.ForecastItem
import com.elderdesktop.model.WeatherResult

@Composable
fun SkyBackground(code: Int) {
    val gradient = when (code) {
        0, 1 -> Brush.verticalGradient(listOf(Color(0xFF4FAAFF), Color(0xFF8ED1FF))) // Clear
        2, 3 -> Brush.verticalGradient(listOf(Color(0xFF7BAED4), Color(0xFFA6C5DB))) // Partly Cloudy
        45, 48 -> Brush.verticalGradient(listOf(Color(0xFF9E9E9E), Color(0xFFBDC3C7))) // Fog
        in 51..67, in 80..82 -> Brush.verticalGradient(listOf(Color(0xFF5D6D7E), Color(0xFF85929E))) // Rain/Drizzle
        in 71..77 -> Brush.verticalGradient(listOf(Color(0xFFD5D8DC), Color(0xFFABB2B9))) // Snow
        95, 96, 99 -> Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF4A235A))) // Thunderstorm
        else -> Brush.verticalGradient(listOf(Color(0xFF5DADE2), Color(0xFFAED6F1)))
    }
    Box(modifier = Modifier.fillMaxSize().background(gradient))
}

@Composable
fun WeatherDetailScreen(result: WeatherResult?) {
    if (result == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.loading), color = Color.White)
        }
    } else if (result.errorMessage != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.weather_error, result.errorMessage),
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 20.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                if (result.cityName.isNotEmpty()) {
                    Text(
                        text = result.cityName,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (result.formattedTemp.isNotEmpty()) {
                    Text(
                        text = result.formattedTemp,
                        fontSize = 100.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                }
                if (result.description.isNotEmpty()) {
                    Text(
                        text = result.description,
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                if (result.isAlert) {
                    Text(
                        text = stringResource(R.string.weather_alert),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }

            items(result.forecast) { item ->
                ForecastRow(item)
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ForecastRow(item: ForecastItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.date,
            modifier = Modifier.weight(1.5f),
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = item.description,
            modifier = Modifier.weight(2f),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp
        )
        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.End
        ) {
            Text(text = item.maxTemp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item.minTemp, color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
        }
    }
}

package com.elderdesktop.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    private var weatherResult by mutableStateOf<WeatherUtils.WeatherResult?>(null)
    private val client = OkHttpClient()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if ((permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
            (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true)
        ) {
            fetchWeatherData()
        }
    }

    @SuppressLint("SourceLockedOrientation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        checkLocationPermissions()
        setContent {
            WeatherAppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    SkyBackground(weatherResult?.weatherCode ?: 0)
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        WeatherScreen(weatherResult)
                    }
                }
            }
        }
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchWeatherData()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchWeatherData() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: android.location.Location? = null

        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                bestLocation = loc
            }
        }

        bestLocation?.let { location ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = WeatherUtils.fetchWeather(location, this@MainActivity, client)
                withContext(Dispatchers.Main) {
                    weatherResult = result
                }
            }
        }
    }
}

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
fun WeatherScreen(result: WeatherUtils.WeatherResult?) {
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 48.dp, bottom = 32.dp)
        ) {
            item {
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
fun ForecastRow(item: WeatherUtils.ForecastItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = item.date, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1.5f))
        Text(text = item.description, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, modifier = Modifier.weight(2f))
        Row(modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.End) {
            Text(text = item.maxTemp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item.minTemp, color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
        }
    }
}

@Composable
fun WeatherAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            background = Color.Black
        ),
        content = content
    )
}

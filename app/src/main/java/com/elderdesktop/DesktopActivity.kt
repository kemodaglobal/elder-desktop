package com.elderdesktop

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.elderdesktop.model.AppInfo
import com.elderdesktop.ui.DesktopLayout
import com.elderdesktop.ui.theme.ElderDesktopTheme
import com.elderdesktop.util.WeatherUtils
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class DesktopActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private var weatherJob: Job? = null
    private var weatherText by mutableStateOf("")
    private var isWeatherAlert by mutableStateOf(false)
    private var locationCity by mutableStateOf("")
    private var currentTemperature by mutableStateOf("")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            startWeatherUpdates()
        }
    }

    private val client = okhttp3.OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        tts = TextToSpeech(this, this)
        
        enableEdgeToEdge()
        
        checkLocationPermissions()
        
        setContent {
            ElderDesktopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    DesktopLayout(
                        onAppLaunch = { app: AppInfo -> speak(app.label) },  // 显式声明类型
                        onSpeak = { text: String -> speak(text) },
                        weatherText = weatherText,
                        isWeatherAlert = isWeatherAlert,
                        locationCity = locationCity,
                        currentTemperature = currentTemperature
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        weatherJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startWeatherUpdates()
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

    private fun startWeatherUpdates() {
        weatherJob?.cancel()
        weatherJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateWeather()
                delay((30 * 60 * 1000L).milliseconds)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateWeather() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)

        if (providers.isEmpty()) {
            Log.w("DesktopActivity", "No location providers available")
            return
        }

        var bestLocation: android.location.Location? = null
        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null &&
                (bestLocation == null || location.accuracy < bestLocation.accuracy)
            ) {
                bestLocation = location
            }
        }

        val location = bestLocation
        if (location != null) {
            fetchWeatherForLocation(location)
        } else {
            Log.w("DesktopActivity", "Could not determine last known location")
        }
    }

    private fun fetchWeatherForLocation(location: android.location.Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = WeatherUtils.fetchWeather(
                    location = location,
                    context = this@DesktopActivity,
                    client = client
                )

                withContext(Dispatchers.Main) {
                    weatherText = result.description
                    locationCity = result.cityName
                    isWeatherAlert = result.isAlert
                    currentTemperature = result.formattedTemp
                }
            } catch (e: Exception) {
                Log.e("DesktopActivity", "Error updating weather", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = java.util.Locale.getDefault()
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}
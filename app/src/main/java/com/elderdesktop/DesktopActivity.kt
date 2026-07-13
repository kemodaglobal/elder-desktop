package com.elderdesktop

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.elderdesktop.model.AppInfo
import com.elderdesktop.ui.DesktopLayout
import com.elderdesktop.ui.theme.ElderDesktopTheme
import com.elderdesktop.util.WeatherUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class DesktopActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private var weatherJob: Job? = null
    private var weatherText by mutableStateOf("")
    private var isWeatherAlert by mutableStateOf(false)
    private var locationCity by mutableStateOf("")
    private var currentTemperature by mutableStateOf("")

    private var triggerSettingsUnlock by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            startWeatherUpdates()
        }
    }

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val command = matches[0].lowercase()
                handleVoiceCommand(command)
            }
        }
    }

    private fun handleVoiceCommand(command: String) {
        val context = this
        when {
            command.contains("相机") || command.contains("拍照") || command.contains("camera") -> {
                speak(getString(R.string.opening_camera))
                com.elderdesktop.util.AppUtils.launchCamera(context)
            }
            command.contains("设置") || command.contains("settings") -> {
                val settings = DesktopSettings(context)
                if (settings.usePasscode) {
                    speak(getString(R.string.please_unlock_first))
                    triggerSettingsUnlock = true
                } else {
                    speak(getString(R.string.opening_desktop_settings))
                    startActivity(Intent(context, SettingsActivity::class.java))
                }
            }
            command.contains("天气") || command.contains("weather") -> {
                speak(getString(R.string.opening_weather))
                startActivity(Intent(context, WeatherActivity::class.java))
            }
            command.contains("打电话") || command.contains("拨号") || command.contains("call") -> {
                speak(getString(R.string.opening_phone))
                val dialerIntent = Intent(Intent.ACTION_DIAL)
                startActivity(dialerIntent)
            }
            else -> {
                speak(getString(R.string.command_not_recognized, command))
            }
        }
    }

    private fun startVoiceEngine() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.voice_not_supported), Toast.LENGTH_SHORT).show()
        }
    }

    private val client = okhttp3.OkHttpClient()

    @SuppressLint("SourceLockedOrientation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLargeScreen = resources.configuration.smallestScreenWidthDp >= 600
        // Lock to portrait only on small screens (phones)
        // This avoids the Chrome OS lint warning and respects Android 17 large-screen policies
        if (!isLargeScreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
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
                            onAppLaunch = { app: AppInfo -> speak(app.label) },
                            onSpeak = { text: String -> speak(text) },
                            onVoiceAssistant = { startVoiceEngine() },
                            weatherText = weatherText,
                            isWeatherAlert = isWeatherAlert,
                            locationCity = locationCity,
                            currentTemperature = currentTemperature,
                            triggerSettingsUnlock = triggerSettingsUnlock,
                            onSettingsUnlockHandled = { triggerSettingsUnlock = false },
                            onRefreshWeather = { updateWeather() }
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
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null &&
                (bestLocation == null || loc.accuracy < bestLocation.accuracy)
            ) {
                bestLocation = loc
            }
        }

        val currentBest = bestLocation
        if (currentBest != null) {
            fetchWeatherForLocation(currentBest)
        } else {
            Log.w("DesktopActivity", "Could not determine last known location, trying manual locations")
            fetchWeatherForManualLocations()
        }
    }

    private fun fetchWeatherForManualLocations() {
        val settings = DesktopSettings(this)
        val manualLocations = settings.manualWeatherLocations
        if (manualLocations.isNotEmpty()) {
            // Try the first manual location for now
            val firstLocation = manualLocations.first()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = WeatherUtils.fetchWeatherForCity(
                        cityName = firstLocation,
                        context = this@DesktopActivity,
                        client = client
                    )

                    if (result != null) {
                        withContext(Dispatchers.Main) {
                            weatherText = result.description
                            locationCity = result.cityName
                            isWeatherAlert = result.isAlert
                            currentTemperature = result.formattedTemp
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DesktopActivity", "Error updating weather for manual location", e)
                }
            }
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

                if (result != null) {
                    withContext(Dispatchers.Main) {
                        weatherText = result.description
                        locationCity = result.cityName
                        isWeatherAlert = result.isAlert
                        currentTemperature = result.formattedTemp
                    }
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
        val settings = DesktopSettings(this)
        if (settings.voiceAnnouncements) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}

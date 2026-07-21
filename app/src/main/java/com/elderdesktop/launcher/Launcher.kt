package com.elderdesktop.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.PermissionRationaleProvider
import com.elderdesktop.ui.DesktopLayout
import com.elderdesktop.ui.PermissionPurposePrompt
import com.elderdesktop.ui.theme.ElderDesktopTheme
import com.elderdesktop.util.DeviceStateHelper
import com.elderdesktop.util.WeatherUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class Launcher : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var weatherJob: Job? = null
    private var triggerSettingsUnlock by mutableStateOf(false)
    private val client = okhttp3.OkHttpClient()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        PermissionRationaleProvider.rationaleMessage = null
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
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                VoiceCommandHandler.handleVoiceCommand(
                    command = matches[0].lowercase(),
                    context = this,
                    client = client,
                    speak = { speak(it) },
                    onTriggerUnlock = { triggerSettingsUnlock = true }
                )
            }
        }
    }

    @SuppressLint("SourceLockedOrientation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasAdvertisingSDK()) {
            Toast.makeText(this, getString(R.string.unauthorized_sdk_warning), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tts = TextToSpeech(this, this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        enableEdgeToEdge()
        window.setBackgroundDrawableResource(android.R.color.transparent)

        DeviceStateHelper.updateOrientationLock(this)
        checkLocationPermissions()

        setContent {
            ElderDesktopTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Transparent) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val weather = WeatherUtils.cachedWeather
                        DesktopLayout(
                            onAppLaunch = { app: AppInfo -> speak(app.label) },
                            onSpeak = { text: String -> speak(text) },
                            onVoiceAssistant = { startVoiceEngine() },
                            weatherText = weather?.description ?: "",
                            isWeatherAlert = weather?.isAlert ?: false,
                            locationCity = weather?.cityName ?: "",
                            currentTemperature = weather?.formattedTemp ?: "",
                            weatherCode = weather?.weatherCode ?: 800,
                            triggerSettingsUnlock = triggerSettingsUnlock,
                            onSettingsUnlockHandled = { triggerSettingsUnlock = false },
                            onRefreshWeather = { updateWeather() }
                        )

                        PermissionPurposePrompt(
                            rationale = PermissionRationaleProvider.rationaleMessage,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        DeviceStateHelper.updateOrientationLock(this)
        DeviceStateHelper.updateWakeLock(this, DesktopSettings(this))
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        DeviceStateHelper.updateOrientationLock(this)
    }

    override fun onPause() {
        super.onPause()
        // WakeLock flags are automatic
    }

    override fun onDestroy() {
        weatherJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                startWeatherUpdates()
            }
            else -> {
                PermissionRationaleProvider.rationaleMessage = getString(R.string.permission_location_purpose)
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        if (providers.isEmpty()) return

        var bestLocation: android.location.Location? = null
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                bestLocation = loc
            }
        }

        bestLocation?.let { fetchWeatherForLocation(it) } ?: fetchWeatherForManualLocations()
    }

    private fun fetchWeatherForManualLocations() {
        val settings = DesktopSettings(this)
        val manualLocations = settings.manualWeatherLocations
        if (manualLocations.isNotEmpty()) {
            val firstLocation = manualLocations.first()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = WeatherUtils.fetchWeatherForCity(cityName = firstLocation, context = this@Launcher, client = client)
                    withContext(Dispatchers.Main) {
                        if (result.errorMessage != null) {
                            Toast.makeText(this@Launcher, result.errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Launcher", "Error updating weather for manual location", e)
                }
            }
        }
    }

    private fun fetchWeatherForLocation(location: android.location.Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = WeatherUtils.fetchWeather(location = location, context = this@Launcher, client = client)
                withContext(Dispatchers.Main) {
                    if (result.errorMessage != null) {
                        Toast.makeText(this@Launcher, result.errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Launcher", "Error updating weather", e)
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = java.util.Locale.getDefault()
        }
    }

    private fun speak(text: String) {
        val settings = DesktopSettings(this)
        if (settings.voiceAnnouncements) {
            tts?.setSpeechRate(settings.speechRate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun hasAdvertisingSDK(): Boolean {
        val adClasses = listOf(
            "com.google.android.gms.ads.AdView", "com.unity3d.ads.UnityAds", "com.applovin.sdk.AppLovinSdk",
            "com.mbridge.msdk.out.MBridgeSDKFactory", "com.facebook.ads.AdView", "com.bytedance.sdk.openadsdk.TTAdSdk", "com.vungle.warren.Vungle"
        )
        for (className in adClasses) {
            try {
                Class.forName(className)
                return true
            } catch (_: ClassNotFoundException) { }
        }
        return false
    }
}

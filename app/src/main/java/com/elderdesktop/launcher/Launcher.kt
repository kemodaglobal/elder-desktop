package com.elderdesktop.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.SettingsActivity
import com.elderdesktop.WeatherActivity
import com.elderdesktop.model.AppInfo
import com.elderdesktop.ui.DesktopLayout
import com.elderdesktop.ui.theme.ElderDesktopTheme
import com.elderdesktop.util.DeepSeekUtils
import com.elderdesktop.util.GoogleAiUtils
import com.elderdesktop.util.OpenAiUtils
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
    private var weatherText by mutableStateOf("")
    private var isWeatherAlert by mutableStateOf(false)
    private var locationCity by mutableStateOf("")
    private var currentTemperature by mutableStateOf("")
    private var weatherCode by mutableIntStateOf(800) // Default clear

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
        val cameraKeywords = getString(R.string.keyword_camera).split(",")
        val settingsKeywords = getString(R.string.keyword_settings).split(",")
        val weatherKeywords = getString(R.string.keyword_weather).split(",")
        val callKeywords = getString(R.string.keyword_call).split(",")
        val contactKeywords = getString(R.string.keyword_contacts).split(",")
        val callPrefixes = getString(R.string.keyword_call_prefix).split(",")

        when {
            cameraKeywords.any { command.contains(it) } -> {
                speak(getString(R.string.opening_camera))
                com.elderdesktop.util.AppUtils.launchCamera(context)
            }
            settingsKeywords.any { command.contains(it) } -> {
                val settings = DesktopSettings(context)
                if (settings.usePasscode) {
                    speak(getString(R.string.please_unlock_first))
                    triggerSettingsUnlock = true
                } else {
                    speak(getString(R.string.opening_desktop_settings))
                    startActivity(Intent(context, SettingsActivity::class.java))
                }
            }
            weatherKeywords.any { command.contains(it) } -> {
                speak(getString(R.string.opening_weather))
                startActivity(Intent(context, WeatherActivity::class.java))
            }
            contactKeywords.any { command.contains(it) && !callKeywords.any { c -> command.contains(c) } } -> {
                speak(getString(R.string.opening_phone)) // Re-using opening_phone or add opening_contacts
                com.elderdesktop.util.AppUtils.launchContacts(context)
            }
            callKeywords.any { command.contains(it) } -> {
                val settings = DesktopSettings(context)
                var targetName = ""

                for (pattern in callPrefixes) {
                    if (command.contains(pattern)) {
                        // Handle "Call [Name]"
                        val after = command.substringAfter(pattern).trim()
                        if (after.isNotEmpty()) {
                            targetName = after
                            break
                        }
                        // Handle "[Name] Call" (for Japanese/Korean suffix-like patterns)
                        val before = command.substringBefore(pattern).trim()
                        if (before.isNotEmpty()) {
                            targetName = before
                            break
                        }
                    }
                }

                if (targetName.isNotEmpty()) {
                    var foundNumber: String? = null
                    // Iterate through all possible speed dial slots
                    for (i in 0 until 30) {
                        val contact = settings.getSpeedDial(i)
                        if (contact != null && contact.first.lowercase() == targetName) {
                            foundNumber = contact.second
                            break
                        }
                    }

                    if (foundNumber != null) {
                        speak(getString(R.string.calling_name, targetName))
                        val intent = Intent(Intent.ACTION_CALL, "tel:$foundNumber".toUri())
                        try {
                            startActivity(intent)
                        } catch (_: Exception) {
                            // Fallback to dialer if CALL_PHONE permission is missing
                            val dialIntent = Intent(Intent.ACTION_DIAL, "tel:$foundNumber".toUri())
                            startActivity(dialIntent)
                        }
                    } else {
                        speak(getString(R.string.contact_not_found, targetName))
                    }
                } else {
                    speak(getString(R.string.opening_phone))
                    com.elderdesktop.util.AppUtils.launchDialer(context)
                }
            }
            else -> {
                val settings = DesktopSettings(context)
                if (settings.enableDeepSeek) {
                    speak(getString(R.string.loading))
                    CoroutineScope(Dispatchers.IO).launch {
                        val reply = when (settings.aiProvider) {
                            "openai" -> OpenAiUtils.chat(
                                prompt = command,
                                apiKey = settings.openAiApiKey,
                                client = client
                            )
                            "google" -> GoogleAiUtils.chat(
                                prompt = command,
                                apiKey = settings.googleAiApiKey,
                                client = client
                            )
                            else -> DeepSeekUtils.chat(
                                prompt = command,
                                apiKey = settings.deepSeekApiKey,
                                client = client
                            )
                        }
                        withContext(Dispatchers.Main) {
                            if (reply != null) {
                                speak(reply)
                            } else {
                                speak(getString(R.string.command_not_recognized, command))
                            }
                        }
                    }
                } else {
                    speak(getString(R.string.command_not_recognized, command))
                }
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

        // Runtime check for unauthorized Advertising SDKs (Enforcing open-source policy)
        if (hasAdvertisingSDK()) {
            Toast.makeText(this, "Unauthorized Advertising SDK detected. Please use the original open-source version.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tts = TextToSpeech(this, this)

        enableEdgeToEdge()

        updateOrientationLock()

        checkLocationPermissions()

        setContent {
            ElderDesktopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    DesktopLayout(
                        onAppLaunch = { app: AppInfo -> speak(app.label) },
                        onSpeak = { text: String -> speak(text) },
                        onVoiceAssistant = { startVoiceEngine() },
                        weatherText = weatherText,
                        isWeatherAlert = isWeatherAlert,
                        locationCity = locationCity,
                        currentTemperature = currentTemperature,
                        weatherCode = weatherCode,
                        triggerSettingsUnlock = triggerSettingsUnlock,
                        onSettingsUnlockHandled = { triggerSettingsUnlock = false },
                        onRefreshWeather = { updateWeather() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateOrientationLock()
        updateWakeLock()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientationLock()
    }

    override fun onPause() {
        super.onPause()
        releaseWakeLock()
    }

    override fun onDestroy() {
        weatherJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun updateWakeLock() {
        val settings = DesktopSettings(this)
        if (settings.preventSleep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false)
                setTurnScreenOn(false)
            } else {
                @Suppress("DEPRECATION")
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        }
    }

    private fun releaseWakeLock() {
        // Flags are managed automatically by the window lifecycle
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
            Log.w("Launcher", "No location providers available")
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
            Log.w("Launcher", "Could not determine last known location, trying manual locations")
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
                        context = this@Launcher,
                        client = client
                    )

                    withContext(Dispatchers.Main) {
                        if (result.errorMessage != null) {
                            weatherText = result.errorMessage
                            isWeatherAlert = true
                        } else {
                            weatherText = result.description
                            locationCity = result.cityName
                            isWeatherAlert = result.isAlert
                            currentTemperature = result.formattedTemp
                            weatherCode = result.weatherCode
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
                val result = WeatherUtils.fetchWeather(
                    location = location,
                    context = this@Launcher,
                    client = client
                )

                withContext(Dispatchers.Main) {
                    if (result.errorMessage != null) {
                        weatherText = result.errorMessage
                        isWeatherAlert = true
                    } else {
                        weatherText = result.description
                        locationCity = result.cityName
                        isWeatherAlert = result.isAlert
                        currentTemperature = result.formattedTemp
                        weatherCode = result.weatherCode
                    }
                }
            } catch (e: Exception) {
                Log.e("Launcher", "Error updating weather", e)
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
            tts?.setSpeechRate(settings.speechRate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun updateOrientationLock() {
        val isLargeScreen = resources.configuration.smallestScreenWidthDp >= 600
        if (!isLargeScreen) {
            // Lock to portrait only on small screens (phones)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            // Allow rotation on large screens (tablets/unfolded foldables)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun hasAdvertisingSDK(): Boolean {
        val adClasses = listOf(
            "com.google.android.gms.ads.AdView",
            "com.unity3d.ads.UnityAds",
            "com.applovin.sdk.AppLovinSdk",
            "com.mbridge.msdk.out.MBridgeSDKFactory",
            "com.facebook.ads.AdView",
            "com.bytedance.sdk.openadsdk.TTAdSdk", // Pangle
            "com.vungle.warren.Vungle"
        )
        for (className in adClasses) {
            try {
                Class.forName(className)
                Log.e("ElderDesktop", "CRITICAL: Unauthorized Advertising SDK detected: $className")
                return true
            } catch (_: ClassNotFoundException) {
                // Class not present, which is good
            }
        }
        return false
    }
}

package com.elderdesktop

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class DesktopSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("desktop_settings", Context.MODE_PRIVATE)

    var layoutRows: Int
        get() = prefs.getInt("layout_rows", 4)
        set(value) = prefs.edit { putInt("layout_rows", value) }

    var layoutCols: Int
        get() = prefs.getInt("layout_cols", 2)
        set(value) = prefs.edit { putInt("layout_cols", value) }

    var showVoiceAssistant: Boolean
        get() = prefs.getBoolean("show_voice_assistant", false)
        set(value) = prefs.edit { putBoolean("show_voice_assistant", value) }

    var voiceAssistantMode: Int
        get() = prefs.getInt("voice_assistant_mode", 0) // 0: System, 1: Engine
        set(value) = prefs.edit { putInt("voice_assistant_mode", value) }

    var voiceAnnouncements: Boolean
        get() = prefs.getBoolean("voice_announcements", true)
        set(value) = prefs.edit { putBoolean("voice_announcements", value) }

    var speechRate: Float
        get() = prefs.getFloat("speech_rate", 1.0f)
        set(value) = prefs.edit { putFloat("speech_rate", value) }

    var usePasscode: Boolean
        get() = prefs.getBoolean("use_passcode", false)
        set(value) = prefs.edit { putBoolean("use_passcode", value) }

    var isBasicMode: Boolean
        get() = prefs.getBoolean("is_basic_mode", false)
        set(value) = prefs.edit { putBoolean("is_basic_mode", value) }

    var passcode: String
        get() = prefs.getString("passcode", "") ?: ""
        set(value) = prefs.edit { putString("passcode", value) }

    var selectedApps: Set<String>
        get() = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("selected_apps", value) }

    var fontSizeMultiplier: Float
        get() = prefs.getFloat("font_size_multiplier", 1.0f)
        set(value) = prefs.edit { putFloat("font_size_multiplier", value) }

    var iconSizeMultiplier: Float
        get() = prefs.getFloat("icon_size_multiplier", 1.0f)
        set(value) = prefs.edit { putFloat("icon_size_multiplier", value) }

    var iconShape: String
        get() = prefs.getString("icon_shape", "rounded") ?: "rounded" // rounded, circle, square, native
        set(value) = prefs.edit { putString("icon_shape", value) }

    var highContrastMode: Boolean
        get() = prefs.getBoolean("high_contrast_mode", false)
        set(value) = prefs.edit { putBoolean("high_contrast_mode", value) }

    var themeChoice: String
        get() = prefs.getString("theme_choice", "classic") ?: "classic" // classic, emerald, rose, orange
        set(value) = prefs.edit { putString("theme_choice", value) }

    var fontChoice: String
        get() = prefs.getString("font_choice", "default") ?: "default" // default, serif, monospace
        set(value) = prefs.edit { putString("font_choice", value) }

    var enableDesktopNotifications: Boolean
        get() = prefs.getBoolean("enable_desktop_notifications", false)
        set(value) = prefs.edit { putBoolean("enable_desktop_notifications", value) }

    var preventSleep: Boolean
        get() = prefs.getBoolean("prevent_sleep", false)
        set(value) = prefs.edit { putBoolean("prevent_sleep", value) }

    var manualWeatherLocations: Set<String>
        get() = prefs.getStringSet("manual_weather_locations", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("manual_weather_locations", value) }

    var weatherProvider: String
        get() = prefs.getString("weather_provider", "open-meteo") ?: "open-meteo" // open-meteo, qweather, openweather
        set(value) = prefs.edit { putString("weather_provider", value) }

    var weatherApiKey: String
        get() = prefs.getString("weather_api_key", "") ?: ""
        set(value) = prefs.edit { putString("weather_api_key", value) }

    var privacyAccepted: Boolean
        get() = prefs.getBoolean("privacy_accepted", false)
        set(value) = prefs.edit { putBoolean("privacy_accepted", value) }

    var enableDeepSeek: Boolean
        get() = prefs.getBoolean("enable_deepseek", false)
        set(value) = prefs.edit { putBoolean("enable_deepseek", value) }

    var deepSeekApiKey: String
        get() = prefs.getString("deepseek_api_key", "") ?: ""
        set(value) = prefs.edit { putString("deepseek_api_key", value) }

    var aiProvider: String
        get() = prefs.getString("ai_provider", "deepseek") ?: "deepseek" // deepseek, openai, google
        set(value) = prefs.edit { putString("ai_provider", value) }

    var openAiApiKey: String
        get() = prefs.getString("openai_api_key", "") ?: ""
        set(value) = prefs.edit { putString("openai_api_key", value) }

    var googleAiApiKey: String
        get() = prefs.getString("google_ai_api_key", "") ?: ""
        set(value) = prefs.edit { putString("google_ai_api_key", value) }

    fun addWeatherLocation(location: String) {
        val locations = manualWeatherLocations.toMutableSet()
        if (locations.size < 6) {
            locations.add(location)
            manualWeatherLocations = locations
        }
    }

    fun removeWeatherLocation(location: String) {
        val locations = manualWeatherLocations.toMutableSet()
        locations.remove(location)
        manualWeatherLocations = locations
    }

    fun getSpeedDial(index: Int): Triple<String, String, String?>? {
        val name = prefs.getString("speed_dial_${index}_name", null) ?: return null
        val number = prefs.getString("speed_dial_${index}_number", "") ?: ""
        val photoUri = prefs.getString("speed_dial_${index}_photo", null)
        return Triple(name, number, photoUri)
    }

    fun setSpeedDial(index: Int, name: String, number: String, photoUri: String? = null) {
        prefs.edit {
            putString("speed_dial_${index}_name", name)
            putString("speed_dial_${index}_number", number)
            if (photoUri != null) {
                putString("speed_dial_${index}_photo", photoUri)
            } else {
                remove("speed_dial_${index}_photo")
            }
        }
    }

    fun clearSpeedDial(index: Int) {
        prefs.edit {
            remove("speed_dial_${index}_name")
            remove("speed_dial_${index}_number")
            remove("speed_dial_${index}_photo")
        }
    }

    fun use2x3() {
        layoutRows = 4
        layoutCols = 2
    }
    
    fun use3x4() {
        layoutRows = 5
        layoutCols = 3
    }

    fun use3x2() {
        layoutRows = 3
        layoutCols = 3
    }

    fun use4x3() {
        layoutRows = 4
        layoutCols = 4
    }

    fun use6x4() {
        layoutRows = 5
        layoutCols = 6
    }
    
    val is2x3: Boolean get() = layoutCols == 2 && layoutRows == 4
    val is3x4: Boolean get() = layoutCols == 3 && layoutRows == 5
    val is3x2: Boolean get() = layoutCols == 3 && layoutRows == 3
    val is4x3: Boolean get() = layoutCols == 4 && layoutRows == 4
    val is6x4: Boolean get() = layoutCols == 6 && layoutRows == 5
}

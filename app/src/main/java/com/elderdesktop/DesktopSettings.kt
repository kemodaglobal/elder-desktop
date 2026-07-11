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

    var usePasscode: Boolean
        get() = prefs.getBoolean("use_passcode", false)
        set(value) = prefs.edit { putBoolean("use_passcode", value) }

    var passcode: String
        get() = prefs.getString("passcode", "") ?: ""
        set(value) = prefs.edit { putString("passcode", value) }

    var selectedApps: Set<String>
        get() = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("selected_apps", value) }

    fun getSpeedDial(index: Int): Pair<String, String>? {
        val name = prefs.getString("speed_dial_${index}_name", null) ?: return null
        val number = prefs.getString("speed_dial_${index}_number", "") ?: ""
        return Pair(name, number)
    }

    fun setSpeedDial(index: Int, name: String, number: String) {
        prefs.edit {
            putString("speed_dial_${index}_name", name)
                .putString("speed_dial_${index}_number", number)
        }
    }

    fun clearSpeedDial(index: Int) {
        prefs.edit {
            remove("speed_dial_${index}_name")
                .remove("speed_dial_${index}_number")
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
    
    val is2x3: Boolean get() = layoutCols == 2 && layoutRows == 4
}

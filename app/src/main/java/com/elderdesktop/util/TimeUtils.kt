package com.elderdesktop.util

import android.content.Context
import com.elderdesktop.R
import java.util.Locale

object TimeUtils {
    fun getTimeOfDayMarker(context: Context, locale: Locale, hour: Int): String {
        return if (locale.language == "zh") {
            when (hour) {
                in 0..0 -> context.getString(R.string.time_midnight)
                in 1..5 -> context.getString(R.string.time_dawn)
                in 6..11 -> context.getString(R.string.time_morning)
                in 12..12 -> context.getString(R.string.time_noon)
                in 13..18 -> context.getString(R.string.time_afternoon)
                else -> context.getString(R.string.time_evening)
            }
        } else {
            if (hour < 12) "AM" else "PM"
        }
    }
}

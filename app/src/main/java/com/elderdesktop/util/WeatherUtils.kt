package com.elderdesktop.util

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.elderdesktop.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

object WeatherUtils {

    fun getWeatherDescription(code: Int, context: Context): String {
        val resId = when (code) {
            0 -> R.string.weather_clear
            1, 2, 3 -> R.string.weather_partly_cloudy
            45, 48 -> R.string.weather_fog
            51, 53, 55 -> R.string.weather_drizzle
            61, 63, 65 -> R.string.weather_rain
            71, 73, 75 -> R.string.weather_snow
            80, 81, 82 -> R.string.weather_rain_showers
            95, 96, 99 -> R.string.weather_thunderstorm
            else -> R.string.weather_cloudy
        }
        return context.getString(resId)
    }

    fun formatTemperature(tempCelsius: Double, context: Context): String {
        val locale = Locale.getDefault()
        val country = locale.country
        val fahrenheitCountries = setOf("US", "BS", "KY", "PW", "BZ", "PR")

        return if (country in fahrenheitCountries) {
            val tempF = (tempCelsius * 9 / 5 + 32).toInt()
            "${tempF}°F"
        } else {
            "${tempCelsius.toInt()}°C"
        }
    }

    /**
     * 获取天气数据
     * @param location 位置
     * @param context 上下文
     * @param client OkHttpClient 实例
     * @return Result<天气描述, 城市名, 是否有警报, 格式化温度>
     */
    data class WeatherResult(
        val description: String,
        val cityName: String,
        val isAlert: Boolean,
        val formattedTemp: String
    )

    suspend fun fetchWeather(
        location: Location,
        context: Context,
        client: OkHttpClient
    ): WeatherResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=${location.latitude}&longitude=${location.longitude}" +
                        "&current_weather=true"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (body != null) {
                    val json = JSONObject(body)
                    val current = json.getJSONObject("current_weather")
                    val temp = current.getDouble("temperature")
                    val code = current.getInt("weathercode")

                    val formattedTemp = formatTemperature(temp, context)
                    val description = getWeatherDescription(code, context)

                    val isHighTemp = temp >= 35
                    val isHeavyRain = code in listOf(65, 82, 95, 96, 99)
                    val isAlert = isHighTemp || isHeavyRain

                    // 获取城市名
                    val cityName = getCityName(context, location.latitude, location.longitude)

                    WeatherResult(description, cityName, isAlert, formattedTemp)
                } else {
                    WeatherResult("", "", false, "")
                }
            } catch (e: Exception) {
                Log.e("WeatherUtils", "Error fetching weather", e)
                WeatherResult("", "", false, "")
            }
        }
    }

    private fun getCityName(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
            } else ""
        } catch (e: Exception) {
            Log.e("WeatherUtils", "Error getting city name", e)
            ""
        }
    }
}
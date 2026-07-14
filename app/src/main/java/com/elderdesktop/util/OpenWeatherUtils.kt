package com.elderdesktop.util

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object OpenWeatherUtils {
    // OpenWeatherMap implementation
    private const val API_URL = "https://api.openweathermap.org/data/2.5/weather"
    
    suspend fun fetchWeather(
        location: Location,
        context: Context,
        apiKey: String,
        client: OkHttpClient
    ): WeatherUtils.WeatherResult {
        if (apiKey.isEmpty()) {
            return WeatherUtils.WeatherResult(
                description = "",
                cityName = "",
                isAlert = false,
                formattedTemp = "",
                errorMessage = context.getString(com.elderdesktop.R.string.weather_error_auth)
            )
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val url = "$API_URL?lat=${location.latitude}&lon=${location.longitude}&appid=$apiKey&units=metric"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body.string()
                    if (body.isNotEmpty()) {
                        val json = JSONObject(body)
                        val main = json.getJSONObject("main")
                        val temp = main.getDouble("temp")
                        val weather = json.getJSONArray("weather").getJSONObject(0)
                        val description = weather.getString("description")
                        
                        val isHighTemp = temp >= 35

                        return@withContext WeatherUtils.WeatherResult(
                            description = description,
                            cityName = json.getString("name"),
                            isAlert = isHighTemp,
                            formattedTemp = WeatherUtils.formatTemperature(temp),
                            weatherCode = 0
                        )
                    }
                } else {
                    val errorResId = when (response.code) {
                        401, 403 -> com.elderdesktop.R.string.weather_error_auth
                        429 -> com.elderdesktop.R.string.weather_error_limit
                        in 500..599 -> com.elderdesktop.R.string.weather_error_server
                        else -> com.elderdesktop.R.string.weather_error_network
                    }
                    return@withContext WeatherUtils.WeatherResult("", "", false, "", errorMessage = context.getString(errorResId))
                }
            } catch (e: Exception) {
                Log.e("OpenWeatherUtils", "Error fetching OpenWeatherMap", e)
                return@withContext WeatherUtils.WeatherResult("", "", false, "", errorMessage = e.localizedMessage)
            }
            WeatherUtils.WeatherResult("", "", false, "", errorMessage = context.getString(com.elderdesktop.R.string.weather_error_no_data))
        }
    }
}

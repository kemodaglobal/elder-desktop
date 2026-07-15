package com.elderdesktop.util

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object QWeatherUtils {
    // QWeather (HeWeather) implementation
    // Using free development key usually targets devapi.qweather.com
    private const val API_URL = "https://devapi.qweather.com/v7/weather/now"
    
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
                // Now weather
                val nowUrl = "$API_URL?location=${location.longitude},${location.latitude}&key=$apiKey"
                val request = Request.Builder().url(nowUrl).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body.string()
                    if (body.isNotEmpty()) {
                        val json = JSONObject(body)
                        val code = json.getString("code")
                        if (code == "200") {
                            val now = json.getJSONObject("now")
                            val temp = now.getDouble("temp")
                            val text = now.getString("text")
                            val iconCode = now.optString("icon", "100").toIntOrNull() ?: 100
                            
                            // Forecast for alerts (simple implementation)
                            val isHighTemp = temp >= 35
                            val isAlert = isHighTemp || text.contains("雨") || text.contains("雪") || text.contains("Storm")
                            
                            return@withContext WeatherUtils.WeatherResult(
                                description = text,
                                cityName = "", // City name resolved in WeatherUtils
                                isAlert = isAlert,
                                formattedTemp = WeatherUtils.formatTemperature(temp),
                                weatherCode = iconCode
                            )
                        } else {
                            val errorMsg = when (code) {
                                "401", "403" -> context.getString(com.elderdesktop.R.string.weather_error_auth)
                                "429" -> context.getString(com.elderdesktop.R.string.weather_error_limit)
                                else -> "API Error: $code"
                            }
                            return@withContext WeatherUtils.WeatherResult("", "", false, "", errorMessage = errorMsg)
                        }
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
                Log.e("QWeatherUtils", "Error fetching QWeather", e)
                return@withContext WeatherUtils.WeatherResult("", "", false, "", errorMessage = e.localizedMessage)
            }
            WeatherUtils.WeatherResult("", "", false, "", errorMessage = context.getString(com.elderdesktop.R.string.weather_error_no_data))
        }
    }
}

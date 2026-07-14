package com.elderdesktop.weather

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

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

    fun formatTemperature(tempCelsius: Double): String {
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

    data class ForecastItem(
        val date: String,
        val maxTemp: String,
        val minTemp: String,
        val description: String
    )

    data class WeatherResult(
        val description: String,
        val cityName: String,
        val isAlert: Boolean,
        val formattedTemp: String,
        val forecast: List<ForecastItem> = emptyList(),
        val weatherCode: Int = 0,
        val errorMessage: String? = null
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
                        "&current_weather=true&daily=weathercode,temperature_2m_max,temperature_2m_min&timezone=auto"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    val errorResId = when (response.code) {
                        401, 403 -> R.string.weather_error_auth
                        429 -> R.string.weather_error_limit
                        in 500..599 -> R.string.weather_error_server
                        else -> R.string.weather_error_network
                    }
                    return@withContext WeatherResult(
                        description = "", cityName = "", isAlert = false, 
                        formattedTemp = "", errorMessage = context.getString(errorResId)
                    )
                }

                val body = response.body.string()
                if (body.isEmpty()) {
                    return@withContext WeatherResult(
                        description = "", cityName = "", isAlert = false, 
                        formattedTemp = "", errorMessage = context.getString(R.string.weather_error_no_data)
                    )
                }

                run {
                    val json = JSONObject(body)

                    // Current weather
                    val current =
                        json.optJSONObject("current_weather") ?: return@withContext WeatherResult(
                            description = "",
                            cityName = "",
                            isAlert = false,
                            formattedTemp = "",
                            errorMessage = context.getString(R.string.weather_error_no_data)
                        )
                    val temp = current.getDouble("temperature")
                    val code = current.getInt("weathercode")

                    val formattedTemp = formatTemperature(temp)
                    val description = getWeatherDescription(code, context)

                    val isHighTemp = temp >= 35
                    val isHeavyRain = code in listOf(65, 82, 95, 96, 99)
                    val isAlert = isHighTemp || isHeavyRain

                    // Daily forecast
                    val daily = json.optJSONObject("daily")
                    val forecastList = mutableListOf<ForecastItem>()
                    if (daily != null) {
                        val times = daily.getJSONArray("time")
                        val dailyCodes = daily.getJSONArray("weathercode")
                        val maxTemps = daily.getJSONArray("temperature_2m_max")
                        val minTemps = daily.getJSONArray("temperature_2m_min")

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

                        for (i in 0 until times.length()) {
                            val dateStr = times.getString(i)
                            val date = dateFormat.parse(dateStr)
                            val formattedDate = if (date != null) displayFormat.format(date) else dateStr

                            forecastList.add(
                                ForecastItem(
                                    date = formattedDate,
                                    maxTemp = formatTemperature(maxTemps.getDouble(i)),
                                    minTemp = formatTemperature(minTemps.getDouble(i)),
                                    description = getWeatherDescription(dailyCodes.getInt(i), context)
                                )
                            )
                        }
                    }

                    val cityName = getCityName(context, location.latitude, location.longitude)

                    WeatherResult(description, cityName, isAlert, formattedTemp, forecastList, code)
                }
            } catch (_: java.net.SocketTimeoutException) {
                WeatherResult("", "", false, "", errorMessage = context.getString(R.string.weather_error_network))
            } catch (_: java.io.IOException) {
                WeatherResult("", "", false, "", errorMessage = context.getString(R.string.weather_error_network))
            } catch (e: Exception) {
                WeatherResult("", "", false, "", errorMessage = e.localizedMessage)
            }
        }
    }

    private suspend fun getCityName(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    val cityName = if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                    } else ""
                    continuation.resume(cityName)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val cityName = if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                } else ""
                continuation.resume(cityName)
            }
        } catch (e: Exception) {
            Log.e("WeatherUtils", "Error getting city name", e)
            continuation.resume("")
        }
    }
}

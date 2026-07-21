package com.elderdesktop.util

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.elderdesktop.R
import com.elderdesktop.model.ForecastItem
import com.elderdesktop.model.WeatherResult
import com.elderdesktop.model.WeatherType
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

    var cachedWeather by mutableStateOf<WeatherResult?>(null)
        private set

    fun getWeatherType(code: Int): WeatherType {
        return when (code) {
            800, 801, 802 -> WeatherType.CLEAR
            803, 804 -> WeatherType.CLOUDY
            in 200..299, in 300..399, in 500..599 -> WeatherType.RAIN
            in 600..699 -> WeatherType.SNOW
            in 700..799 -> WeatherType.ATMOSPHERE

            0, 1, 2, 3 -> WeatherType.CLEAR
            in 51..65, in 80..82, in 95..99 -> WeatherType.RAIN
            in 71..75 -> WeatherType.SNOW
            45, 48 -> WeatherType.ATMOSPHERE
            
            100, 101, 102, 103, 150 -> WeatherType.CLEAR
            104 -> WeatherType.CLOUDY
            
            else -> WeatherType.UNKNOWN
        }
    }

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

    suspend fun fetchWeather(
        location: Location,
        context: Context,
        client: OkHttpClient
    ): WeatherResult {
        val settings = com.elderdesktop.DesktopSettings(context)
        val result: WeatherResult = when (settings.weatherProvider) {
            "qweather" -> {
                if (settings.weatherApiKey.isEmpty()) fetchWeatherOpenMeteo(location, context, client)
                else {
                    val qResult = QWeatherUtils.fetchWeather(location, context, settings.weatherApiKey, client)
                    if (qResult.cityName.isEmpty()) {
                        qResult.copy(cityName = getCityName(context, location.latitude, location.longitude))
                    } else qResult
                }
            }
            "openweather" -> {
                if (settings.weatherApiKey.isEmpty()) fetchWeatherOpenMeteo(location, context, client)
                else OpenWeatherUtils.fetchWeather(location, context, settings.weatherApiKey, client)
            }
            else -> fetchWeatherOpenMeteo(location, context, client)
        }
        
        if (result.errorMessage == null) {
            cachedWeather = result
        }
        return result
    }

    private suspend fun fetchWeatherOpenMeteo(
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

                val json = JSONObject(body)
                val current = json.optJSONObject("current_weather") ?: return@withContext WeatherResult(
                    description = "", cityName = "", isAlert = false, formattedTemp = "", 
                    errorMessage = context.getString(R.string.weather_error_no_data)
                )
                val temp = current.getDouble("temperature")
                val code = current.getInt("weathercode")

                val formattedTemp = formatTemperature(temp)
                val description = getWeatherDescription(code, context)

                val isHighTemp = temp >= 35
                val isHeavyRain = code in listOf(65, 82, 95, 96, 99)
                val isAlert = isHighTemp || isHeavyRain

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
            } catch (e: Exception) {
                WeatherResult("", "", false, "", errorMessage = e.localizedMessage)
            }
        }
    }

    suspend fun fetchWeatherForCity(
        cityName: String,
        context: Context,
        client: OkHttpClient
    ): WeatherResult {
        val location = getLocationFromCityName(context, cityName)
        val result = if (location != null) {
            fetchWeather(location, context, client)
        } else {
            WeatherResult("", "", false, "", errorMessage = context.getString(R.string.weather_error_city_not_found))
        }
        if (result.errorMessage == null) {
            cachedWeather = result
        }
        return result
    }

    private suspend fun getLocationFromCityName(context: Context, cityName: String): Location? = suspendCancellableCoroutine { continuation ->
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(cityName, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        continuation.resume(Location("").apply { latitude = address.latitude; longitude = address.longitude })
                    } else continuation.resume(null)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(cityName, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    continuation.resume(Location("").apply { latitude = address.latitude; longitude = address.longitude })
                } else continuation.resume(null)
            }
        } catch (e: Exception) {
            Log.e("WeatherUtils", "Error geocoding city name", e)
            continuation.resume(null)
        }
    }

    private suspend fun getCityName(context: Context, latitude: Double, longitude: Double): String = suspendCancellableCoroutine { continuation ->
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

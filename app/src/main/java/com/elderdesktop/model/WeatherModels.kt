package com.elderdesktop.model

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

enum class WeatherType {
    CLEAR, CLOUDY, RAIN, SNOW, ATMOSPHERE, UNKNOWN
}

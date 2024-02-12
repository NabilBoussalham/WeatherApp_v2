package com.example.myweatherapp.Forecast

import com.example.myweatherapp.R.raw

data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
) {
    val weatherCondition: String?
        get() = weather.firstOrNull()?.main

    fun getIconResource(): Int {
        return when (weatherCondition) {
            "Clouds" -> raw.cloud
            "Clear" -> raw.sun
            "Rain" -> raw.rain
            "Snow" -> raw.snow
            else -> raw.sun
        }
    }
}

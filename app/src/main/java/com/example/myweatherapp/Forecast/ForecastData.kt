package com.example.myweatherapp.Forecast


data class ForecastData(
    val city: City,
    val list: List<ForecastItem>
)
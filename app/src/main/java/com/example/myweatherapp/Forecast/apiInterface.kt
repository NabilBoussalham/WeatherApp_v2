package com.example.myweatherapp.Forecast
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface apiInterface {
    @GET("forecast?")
    fun getForecastData(
        @Query("q") city: String,
        @Query("appid") appid: String,
        @Query("units") units: String
    ): Call<ForecastData>

}

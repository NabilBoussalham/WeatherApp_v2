package com.example.myweatherapp.Forecast
import android.app.Application
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Weatherapp : Application() {

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

        private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        fun getRetrofitInstance(): Retrofit {
            return retrofit
        }
    }

}

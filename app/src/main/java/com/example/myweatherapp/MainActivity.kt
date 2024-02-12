package com.example.myweatherapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myweatherapp.Forecast.ForecastAdapter
import com.example.myweatherapp.Forecast.ForecastData
import com.example.myweatherapp.Forecast.ForecastItem
import com.example.myweatherapp.Forecast.Weatherapp
import com.example.myweatherapp.Forecast.apiInterface
import com.example.myweatherapp.WeatherNow.ApiInterface
import com.example.myweatherapp.WeatherNow.WeatherApp
import com.example.myweatherapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var forecastDataList: List<ForecastItem> = emptyList()
    private lateinit var listView: ListView
    private val OPEN_WEATHER_MAP_API_KEY = "3d344a40ad6dba03dee8bd14f0d2d047"
    private val API_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private var currentWeatherApp: WeatherApp? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        fetchWeatherData("sidi bennour")

        setupSearchCity()
        listView = findViewById(R.id.forecastListView)
    }


    private fun setupSearchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    fetchWeatherData(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun fetchWeatherData(cityName: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(API_BASE_URL)
            .build().create(ApiInterface::class.java)

        val response = retrofit.getWeatherData(cityName, OPEN_WEATHER_MAP_API_KEY, "metric")
        response.enqueue(object : Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    handleWeatherData(responseBody)
                    getForecastData(cityName)
                    currentWeatherApp = responseBody
                } else {
                    Toast.makeText(this@MainActivity, "City not found. Please enter a valid city name.", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                Toast.makeText(this@MainActivity, "API request failed:", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun handleWeatherData(weatherApp: WeatherApp) {
        val temperature = weatherApp.main.temp
        val condition = weatherApp.weather.firstOrNull()?.main ?: "unknown"
        val maxW = weatherApp.main.temp_max
        val minW = weatherApp.main.temp_min
        val city = weatherApp.name
        val sunrise = weatherApp.sys.sunrise
        val sunset = weatherApp.sys.sunset
        val sea = weatherApp.main.pressure
        val humidity = weatherApp.main.humidity
        val windspeed = weatherApp.wind.speed

        updateUI(temperature, condition, maxW, minW, city, sunrise, sunset, sea, humidity, windspeed)
        changeImg(condition)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(
        temperature: Double,
        condition: String,
        maxW: Double,
        minW: Double,
        city: String,
        sunrise: Long,
        sunset: Long,
        sea: Int,
        humidity: Int,
        windspeed: Double
    ) {
        val sunRise = convertEpochToTime(sunrise)
        val sunSet = convertEpochToTime(sunset)

        binding.WeatherNow.text = "${temperature.toInt()}°C"
        binding.condition.text = condition
        binding.maxetmin.text = "${maxW.toInt()}°/${minW.toInt()}°"
        binding.CityName.text = city
        binding.conditions.text = condition
        binding.humidity.text = "$humidity%"
        binding.sea.text = "$sea mbar"
        binding.sunrise.text = sunRise
        binding.sunset.text = sunSet
        binding.wind.text = "$windspeed Km/h"
    }

    private fun getForecastData(cityNom: String) {
        val apiInterface = Weatherapp.getRetrofitInstance().create(apiInterface::class.java)
        val call = apiInterface.getForecastData(cityNom , "3d344a40ad6dba03dee8bd14f0d2d047", "metric")

        call.enqueue(object : Callback<ForecastData> {
            override fun onResponse(call: Call<ForecastData>, response: Response<ForecastData>) {
                if (response.isSuccessful) {
                    val forecastData = response.body()

                    if (forecastData?.list != null) {
                        forecastDataList = forecastData.list

                        updateUI()
                    } else {
                        Toast.makeText(this@MainActivity, "No forecast data available", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ForecastData>, t: Throwable) {

            }
        })
    }

    private fun updateUI() {
        val adapter = ForecastAdapter(this@MainActivity, forecastDataList)
        listView.adapter = adapter
    }




    private fun convertEpochToTime(epochTime: Long): String {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = Date(epochTime * 1000) // Convert seconds to milliseconds
        return dateFormat.format(date)
    }
    private fun changeImg(condition: String) {
        // Reset the animation before starting it
        binding.img.cancelAnimation()

        when (condition) {
            "Clouds" -> {
                binding.root.setBackgroundResource(R.drawable.colud_background)
                binding.img.setAnimation(R.raw.cloud)
            }
            "Clear" -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.img.setAnimation(R.raw.sun)
            }
            "Rain" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.img.setAnimation(R.raw.rain)
            }
            "Snow" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.img.setAnimation(R.raw.snow)
            }
        }

        // Start the animation
        binding.img.playAnimation()
    }

    private fun showErrorMessage(message: String) {
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        snackbarView.layoutParams = params
        snackbar.show()
    }

}

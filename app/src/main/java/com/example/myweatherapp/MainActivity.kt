package com.example.myweatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myweatherapp.Forecast.ForecastAdapter
import com.example.myweatherapp.Forecast.ForecastData
import com.example.myweatherapp.Forecast.ForecastItem
import com.example.myweatherapp.Forecast.Weatherapp
import com.example.myweatherapp.Forecast.apiInterface
import com.example.myweatherapp.WeatherNow.ApiInterface
import com.example.myweatherapp.WeatherNow.WeatherApp
import com.example.myweatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnSuccessListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val PREFS_FILE_NAME = "MyWeatherAppPrefs"
    private val PREF_KEY_LATITUDE = "latitude"
    private val PREF_KEY_LONGITUDE = "longitude"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var forecastDataList: List<ForecastItem> = emptyList()
    private lateinit var listView: ListView
    private val OPEN_WEATHER_MAP_API_KEY = "3d344a40ad6dba03dee8bd14f0d2d047"
    private val API_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private var currentWeatherApp: WeatherApp? = null
    private val locationPermissionCode = 1
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        }
        val savedLatitude = getDoublePreference(PREF_KEY_LATITUDE)
        val savedLongitude = getDoublePreference(PREF_KEY_LONGITUDE)

        if (savedLatitude != null && savedLongitude != null) {
            // Location exists, fetch weather data with saved location
            fetchWeatherDataByLocation(savedLatitude, savedLongitude)
        } else {
            // Location doesn't exist, request location updates
            setupLocation()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fetchWeatherDataByLocation()
        
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        searchView = findViewById(R.id.searchView)
        swipeRefreshLayout.setOnRefreshListener {
            if (savedLatitude != null && savedLongitude != null) {
            // Location exists, fetch weather data with saved location
            fetchWeatherDataByLocation(savedLatitude, savedLongitude)
        } else {
            // Location doesn't exist, request location updates
            setupLocation()
        }
        setupSearchCity()
        listView = findViewById(R.id.forecastListView)



    }


    private fun setupLocation() {
        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                locationPermissionCode
            )
        } else {
            createLocationRequest()
            createLocationCallback()
            startLocationUpdates()
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // Update location every 10 seconds
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let { location ->
                    // Handle location updates here
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Save the location to SharedPreferences
                    saveDoublePreference(PREF_KEY_LATITUDE, latitude)
                    saveDoublePreference(PREF_KEY_LONGITUDE, longitude)

                    // Call the method to fetch weather data with the new location
                    fetchWeatherDataByLocation(latitude, longitude)
                } ?: run {
                    // If location is not available, set default city to Rabat
                    fetchWeatherData("Rabat")
                }
            }
        }
    }
    private fun saveDoublePreference(key: String, value: Double) {
        val sharedPreferences = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong(key, java.lang.Double.doubleToRawLongBits(value))
        editor.apply()
    }

    private fun getDoublePreference(key: String): Double? {
        val sharedPreferences = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        val rawValue = sharedPreferences.getLong(key, java.lang.Double.doubleToRawLongBits(0.0))
        return java.lang.Double.longBitsToDouble(rawValue)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        LocationServices.getFusedLocationProviderClient(this)
            .removeLocationUpdates(locationCallback)
    }

    private fun setupSearchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    fetchWeatherData(query)
                    hideKeyboard()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        // Handle the "Submit" button press on the soft keyboard
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboard()
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun fetchWeatherDataByLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener(OnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        fetchWeatherDataByLocation(latitude, longitude)
                    }
                })
        }
    }

    private fun fetchWeatherDataByLocation(latitude: Double, longitude: Double) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(API_BASE_URL)
            .build().create(ApiInterface::class.java)

        val response = retrofit.getWeatherDataByLocation(
            latitude, longitude, OPEN_WEATHER_MAP_API_KEY, "metric"
        )

        response.enqueue(object : Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                if (swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false // Stop refreshing indicator
                    searchView.setQuery("", false)
                    searchView.clearFocus()
                }
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    handleWeatherData(responseBody)
                    getForecastData(responseBody.name)
                    currentWeatherApp = responseBody
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "City not found. Please enter a valid city name.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                if (swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false // Stop refreshing indicator
                    searchView.setQuery("", false)
                    searchView.clearFocus()
                }
                Toast.makeText(
                    this@MainActivity,
                    "API request failed: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
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

}

